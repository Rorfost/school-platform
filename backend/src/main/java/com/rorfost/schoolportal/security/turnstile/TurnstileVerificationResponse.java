package com.rorfost.schoolportal.security.turnstile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Response payload returned by the Cloudflare Turnstile siteverify API endpoint.
 *
 * <p>Documentation: https://developers.cloudflare.com/turnstile/get-started/server-side-validation/
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TurnstileVerificationResponse(
        boolean success,
        @JsonProperty("challenge_ts") String challengeTs,
        String hostname,
        @JsonProperty("error-codes") List<String> errorCodes,
        String action,
        String cdata) {}
