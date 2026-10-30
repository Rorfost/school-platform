package com.rorfost.schoolportal.security.turnstile;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * Configuration properties for Cloudflare Turnstile bot and abuse mitigation.
 *
 * <p>The secret key must be set in backend runtime environment variables (e.g. {@code
 * TURNSTILE_SECRET_KEY}) and never exposed to the frontend or version control.
 *
 * <p>Expected hostnames specify the exact browser hostnames permitted to issue challenges (e.g.
 * {@code "dhadhana.workers.dev", "portal.dhadhana.org"}).
 */
@ConfigurationProperties("app.turnstile")
public record TurnstileProperties(
        boolean enabled,
        String secretKey,
        List<String> expectedHostnames,
        Integer connectTimeoutMs,
        Integer readTimeoutMs) {

    public TurnstileProperties {
        if (expectedHostnames == null) {
            expectedHostnames = Collections.emptyList();
        }
        if (connectTimeoutMs == null) {
            connectTimeoutMs = 3000;
        }
        if (readTimeoutMs == null) {
            readTimeoutMs = 5000;
        }
    }
}
