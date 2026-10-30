package com.rorfost.schoolportal.security.turnstile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.common.exception.DomainException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

class TurnstileVerificationServiceTest {

    private ObjectMapper objectMapper;
    private HttpClient httpClient;
    private HttpResponse<String> httpResponse;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        objectMapper = new ObjectMapper();
        httpClient = mock(HttpClient.class);
        httpResponse = (HttpResponse<String>) mock(HttpResponse.class);
    }

    @Test
    void verify_whenDisabled_shouldPassWithoutInvokingHttpClient() {
        TurnstileProperties properties =
                new TurnstileProperties(false, null, List.of(), 1000, 1000);
        TurnstileVerificationService service =
                new TurnstileVerificationService(
                        properties,
                        objectMapper,
                        httpClient,
                        "https://challenges.cloudflare.com/turnstile/v0/siteverify");

        assertThatCode(() -> service.verify(null, "127.0.0.1", "admin_login"))
                .doesNotThrowAnyException();
        verifyNoInteractions(httpClient);
    }

    @Test
    void verify_whenEnabledAndSecretMissing_shouldFailClosed() {
        TurnstileProperties properties = new TurnstileProperties(true, "", List.of(), 1000, 1000);
        TurnstileVerificationService service =
                new TurnstileVerificationService(
                        properties,
                        objectMapper,
                        httpClient,
                        "https://challenges.cloudflare.com/turnstile/v0/siteverify");

        assertThatThrownBy(() -> service.verify("test-token", "127.0.0.1", "admin_login"))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e -> {
                            DomainException de = (DomainException) e;
                            org.assertj.core.api.Assertions.assertThat(de.getStatus())
                                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                            org.assertj.core.api.Assertions.assertThat(de.getCode())
                                    .isEqualTo("turnstile_misconfigured");
                        });
    }

    @Test
    void verify_whenTokenMissing_shouldRejectBadRequest() {
        TurnstileProperties properties =
                new TurnstileProperties(true, "0x4AAAAAAtestSecret", List.of(), 1000, 1000);
        TurnstileVerificationService service =
                new TurnstileVerificationService(
                        properties,
                        objectMapper,
                        httpClient,
                        "https://challenges.cloudflare.com/turnstile/v0/siteverify");

        assertThatThrownBy(() -> service.verify("", "127.0.0.1", "admin_login"))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e -> {
                            DomainException de = (DomainException) e;
                            org.assertj.core.api.Assertions.assertThat(de.getStatus())
                                    .isEqualTo(HttpStatus.BAD_REQUEST);
                            org.assertj.core.api.Assertions.assertThat(de.getCode())
                                    .isEqualTo("turnstile_token_required");
                        });
    }

    @Test
    void verify_whenValidTokenAndMatchingActionAndHostname_shouldSucceed() throws Exception {
        TurnstileProperties properties =
                new TurnstileProperties(
                        true,
                        "0x4AAAAAAtestSecret",
                        List.of("dhadhana.workers.dev", "school.org"),
                        1000,
                        1000);
        TurnstileVerificationService service =
                new TurnstileVerificationService(
                        properties,
                        objectMapper,
                        httpClient,
                        "https://challenges.cloudflare.com/turnstile/v0/siteverify");

        String json =
                """
                {
                    "success": true,
                    "challenge_ts": "2026-10-30T00:00:00.000Z",
                    "hostname": "dhadhana.workers.dev",
                    "action": "admin_login"
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertThatCode(() -> service.verify("valid-token", "198.51.100.1", "admin_login"))
                .doesNotThrowAnyException();
    }

    @Test
    void verify_whenActionMismatches_shouldReject() throws Exception {
        TurnstileProperties properties =
                new TurnstileProperties(true, "0x4AAAAAAtestSecret", List.of(), 1000, 1000);
        TurnstileVerificationService service =
                new TurnstileVerificationService(
                        properties,
                        objectMapper,
                        httpClient,
                        "https://challenges.cloudflare.com/turnstile/v0/siteverify");

        String json =
                """
                {
                    "success": true,
                    "challenge_ts": "2026-10-30T00:00:00.000Z",
                    "hostname": "dhadhana.workers.dev",
                    "action": "other_action"
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertThatThrownBy(() -> service.verify("valid-token", "198.51.100.1", "admin_login"))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e -> {
                            DomainException de = (DomainException) e;
                            org.assertj.core.api.Assertions.assertThat(de.getStatus())
                                    .isEqualTo(HttpStatus.BAD_REQUEST);
                            org.assertj.core.api.Assertions.assertThat(de.getCode())
                                    .isEqualTo("turnstile_action_mismatch");
                        });
    }

    @Test
    void verify_whenHostnameMismatches_shouldReject() throws Exception {
        TurnstileProperties properties =
                new TurnstileProperties(
                        true, "0x4AAAAAAtestSecret", List.of("dhadhana.workers.dev"), 1000, 1000);
        TurnstileVerificationService service =
                new TurnstileVerificationService(
                        properties,
                        objectMapper,
                        httpClient,
                        "https://challenges.cloudflare.com/turnstile/v0/siteverify");

        String json =
                """
                {
                    "success": true,
                    "challenge_ts": "2026-10-30T00:00:00.000Z",
                    "hostname": "attacker.workers.dev",
                    "action": "admin_login"
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertThatThrownBy(() -> service.verify("valid-token", "198.51.100.1", "admin_login"))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e -> {
                            DomainException de = (DomainException) e;
                            org.assertj.core.api.Assertions.assertThat(de.getStatus())
                                    .isEqualTo(HttpStatus.BAD_REQUEST);
                            org.assertj.core.api.Assertions.assertThat(de.getCode())
                                    .isEqualTo("turnstile_hostname_mismatch");
                        });
    }

    @Test
    void verify_whenUpstreamReturnsErrorOrNetworkTimeout_shouldFailClosed() throws Exception {
        TurnstileProperties properties =
                new TurnstileProperties(true, "0x4AAAAAAtestSecret", List.of(), 1000, 1000);
        TurnstileVerificationService service =
                new TurnstileVerificationService(
                        properties,
                        objectMapper,
                        httpClient,
                        "https://challenges.cloudflare.com/turnstile/v0/siteverify");

        when(httpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new IOException("Connection timed out"));

        assertThatThrownBy(() -> service.verify("valid-token", "198.51.100.1", "admin_login"))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e -> {
                            DomainException de = (DomainException) e;
                            org.assertj.core.api.Assertions.assertThat(de.getStatus())
                                    .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                            org.assertj.core.api.Assertions.assertThat(de.getCode())
                                    .isEqualTo("turnstile_verification_unavailable");
                        });
    }

    @Test
    void verify_whenProviderReturnsFalseSuccess_shouldReject() throws Exception {
        TurnstileProperties properties =
                new TurnstileProperties(true, "0x4AAAAAAtestSecret", List.of(), 1000, 1000);
        TurnstileVerificationService service =
                new TurnstileVerificationService(
                        properties,
                        objectMapper,
                        httpClient,
                        "https://challenges.cloudflare.com/turnstile/v0/siteverify");

        String json =
                """
                {
                    "success": false,
                    "error-codes": ["invalid-input-response"]
                }
                """;

        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(json);
        doReturn(httpResponse).when(httpClient).send(any(HttpRequest.class), any());

        assertThatThrownBy(
                        () -> service.verify("expired-or-bad-token", "198.51.100.1", "admin_login"))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e -> {
                            DomainException de = (DomainException) e;
                            org.assertj.core.api.Assertions.assertThat(de.getStatus())
                                    .isEqualTo(HttpStatus.BAD_REQUEST);
                            org.assertj.core.api.Assertions.assertThat(de.getCode())
                                    .isEqualTo("turnstile_verification_failed");
                        });
    }
}
