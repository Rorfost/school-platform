package com.rorfost.schoolportal.security.turnstile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.common.exception.DomainException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;

/**
 * Server-side verification service for Cloudflare Turnstile tokens.
 *
 * <p>Protects abuse-prone endpoints (admin login and public result queries). Validates response
 * token, expected action, and expected browser hostname against the Cloudflare verification endpoint.
 *
 * <p>Fails closed if Turnstile is enabled but the secret key is missing, if the token is missing or
 * invalid, or if the upstream Cloudflare verification service is unavailable.
 */
@Service
@EnableConfigurationProperties(TurnstileProperties.class)
public class TurnstileVerificationService {

    public static final String ACTION_ADMIN_LOGIN = "admin_login";
    public static final String ACTION_EXAM_RESULT_LOOKUP = "exam_result_lookup";
    public static final String TURNSTILE_HEADER = "X-Turnstile-Token";

    private static final Logger log = LoggerFactory.getLogger(TurnstileVerificationService.class);
    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final TurnstileProperties properties;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String verifyEndpointUrl;

    public TurnstileVerificationService(TurnstileProperties properties, ObjectMapper objectMapper) {
        this(
                properties,
                objectMapper,
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(properties.connectTimeoutMs()))
                        .build(),
                VERIFY_URL);
    }

    TurnstileVerificationService(
            TurnstileProperties properties,
            ObjectMapper objectMapper,
            HttpClient httpClient,
            String verifyEndpointUrl) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.verifyEndpointUrl = verifyEndpointUrl;
    }

    /**
     * Verifies a client-supplied Turnstile token for a protected action.
     *
     * @param token Client challenge response token from {@code X-Turnstile-Token}
     * @param remoteIp Client remote IP address (trusted edge IP)
     * @param expectedAction Expected action identifier (e.g. {@code "admin_login"})
     * @throws DomainException if verification fails, token is missing/invalid, or verification service fails
     */
    public void verify(String token, String remoteIp, String expectedAction) {
        if (!properties.enabled()) {
            return;
        }

        String secretKey = properties.secretKey();
        if (secretKey == null || secretKey.isBlank()) {
            // Fail closed in production if enabled but secret is missing
            log.error("Turnstile is enabled but secret key is not configured; failing closed.");
            throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "turnstile_misconfigured");
        }

        if (token == null || token.isBlank()) {
            throw new DomainException(HttpStatus.BAD_REQUEST, "turnstile_token_required");
        }

        StringBuilder formBody = new StringBuilder();
        formBody.append("secret=").append(URLEncoder.encode(secretKey.trim(), StandardCharsets.UTF_8));
        formBody.append("&response=").append(URLEncoder.encode(token.trim(), StandardCharsets.UTF_8));
        if (remoteIp != null && !remoteIp.isBlank()) {
            formBody.append("&remoteip=").append(URLEncoder.encode(remoteIp.trim(), StandardCharsets.UTF_8));
        }

        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(verifyEndpointUrl))
                        .timeout(Duration.ofMillis(properties.readTimeoutMs()))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
                        .build();

        TurnstileVerificationResponse response;
        try {
            HttpResponse<String> httpResponse =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            if (httpResponse.statusCode() != 200) {
                log.warn("Turnstile upstream service returned HTTP status {}", httpResponse.statusCode());
                throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "turnstile_verification_failed");
            }

            response = objectMapper.readValue(httpResponse.body(), TurnstileVerificationResponse.class);
        } catch (DomainException de) {
            throw de;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warn("Turnstile verification call failed due to network/timeout error");
            throw new DomainException(HttpStatus.SERVICE_UNAVAILABLE, "turnstile_verification_unavailable");
        }

        if (response == null || !response.success()) {
            log.info("Turnstile challenge verification rejected (error codes: {})", response != null ? response.errorCodes() : "null");
            throw new DomainException(HttpStatus.BAD_REQUEST, "turnstile_verification_failed");
        }

        // Validate action match
        if (expectedAction != null && !expectedAction.isBlank()) {
            if (response.action() == null || !expectedAction.equalsIgnoreCase(response.action().trim())) {
                log.warn("Turnstile action mismatch: expected='{}', received='{}'", expectedAction, response.action());
                throw new DomainException(HttpStatus.BAD_REQUEST, "turnstile_action_mismatch");
            }
        }

        // Validate hostname against expected hostnames if configured
        List<String> expectedHostnames = properties.expectedHostnames();
        if (expectedHostnames != null && !expectedHostnames.isEmpty()) {
            String receivedHostname = response.hostname() != null ? response.hostname().trim().toLowerCase(Locale.ROOT) : "";
            boolean matched =
                    expectedHostnames.stream()
                            .map(h -> h.trim().toLowerCase(Locale.ROOT))
                            .anyMatch(h -> h.equals(receivedHostname));
            if (!matched) {
                log.warn("Turnstile hostname mismatch: received='{}', expected in={}", receivedHostname, expectedHostnames);
                throw new DomainException(HttpStatus.BAD_REQUEST, "turnstile_hostname_mismatch");
            }
        }
    }
}

