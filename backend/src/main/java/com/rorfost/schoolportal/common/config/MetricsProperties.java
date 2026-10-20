package com.rorfost.schoolportal.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for opt-in request timing metrics and slow-request diagnostics.
 *
 * <p>All instrumentation is disabled by default and must be explicitly enabled via environment
 * variables or profile-specific YAML. No metrics endpoint is exposed to the public; only the
 * Actuator health endpoint remains accessible.
 *
 * <p>Slow-request diagnostic logs are sampled to prevent log cardinality explosion. Sensitive
 * fields (student IDs, school slugs, credentials) are never written to labels or log lines.
 */
@ConfigurationProperties(prefix = "app.metrics")
public record MetricsProperties(

        /**
         * Whether Micrometer-based HTTP counters/timers are active.
         *
         * <p>Keep {@code false} on Render Free to avoid per-metric overhead when no scraper is
         * attached.
         */
        boolean timersEnabled,

        /**
         * Whether slow-request WARN log lines are emitted. Safe to enable in staging for baseline
         * collection.
         */
        boolean slowRequestLoggingEnabled,

        /**
         * Requests that exceed this threshold are emitted as a WARN log when slow-request logging
         * is enabled. Defaults to 2000 ms.
         */
        Duration slowRequestThreshold,

        /**
         * Fraction of slow requests that are actually logged (0.0 – 1.0). Default 0.1 = 10 %,
         * enough to sample a trend without saturating free-tier log quotas.
         */
        double slowRequestSampleRate) {

    public MetricsProperties {
        if (slowRequestThreshold == null) {
            slowRequestThreshold = Duration.ofMillis(2000);
        }
        if (slowRequestSampleRate < 0.0 || slowRequestSampleRate > 1.0) {
            throw new IllegalArgumentException(
                    "app.metrics.slow-request-sample-rate must be between 0.0 and 1.0");
        }
    }

    /** Convenience constructor with safe production defaults (everything off). */
    public MetricsProperties() {
        this(false, false, Duration.ofMillis(2000), 0.1);
    }
}
