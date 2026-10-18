package com.rorfost.schoolportal.auth.application;

import com.rorfost.schoolportal.common.config.SecurityProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LoginRateLimiterTest {

    private static final String TENANT_A = "tenant-a-uuid";
    private static final String TENANT_B = "tenant-b-uuid";
    private static final String USERNAME = "principal@school.edu";
    private static final String IP_1 = "203.0.113.10";
    private static final String IP_2 = "203.0.113.20";

    private String key(String tenant, String ip, String user) {
        return tenant + ":" + ip + ":" + user;
    }

    private SecurityProperties createProperties(int maxAttempts, Duration window) {
        return new SecurityProperties(maxAttempts, window);
    }

    @Nested
    @DisplayName("Rate Limiting Behavior")
    class RateLimitingBehaviorTests {

        @Test
        @DisplayName("checkAllowed does not throw when failure count is below limit")
        void checkAllowed_belowLimit_doesNotThrow() {
            SecurityProperties props = createProperties(3, Duration.ofMinutes(10));
            LoginRateLimiter limiter = new LoginRateLimiter(props);
            String k = key(TENANT_A, IP_1, USERNAME);

            limiter.recordFailure(k);
            limiter.recordFailure(k);

            // Should not throw
            limiter.checkAllowed(k);
        }

        @Test
        @DisplayName("checkAllowed throws LoginRateLimitExceededException when failure count reaches max attempts")
        void checkAllowed_atLimit_throwsException() {
            SecurityProperties props = createProperties(3, Duration.ofMinutes(10));
            LoginRateLimiter limiter = new LoginRateLimiter(props);
            String k = key(TENANT_A, IP_1, USERNAME);

            limiter.recordFailure(k);
            limiter.recordFailure(k);
            limiter.recordFailure(k);

            assertThatThrownBy(() -> limiter.checkAllowed(k))
                    .isInstanceOf(LoginRateLimitExceededException.class);
        }

        @Test
        @DisplayName("clear() resets failure count immediately after successful authentication")
        void clear_resetsFailureCount() {
            SecurityProperties props = createProperties(3, Duration.ofMinutes(10));
            LoginRateLimiter limiter = new LoginRateLimiter(props);
            String k = key(TENANT_A, IP_1, USERNAME);

            limiter.recordFailure(k);
            limiter.recordFailure(k);
            limiter.recordFailure(k);
            assertThatThrownBy(() -> limiter.checkAllowed(k))
                    .isInstanceOf(LoginRateLimitExceededException.class);

            limiter.clear(k);
            limiter.checkAllowed(k);
        }
    }

    @Nested
    @DisplayName("Tenant Separation")
    class TenantSeparationTests {

        @Test
        @DisplayName("Failures under Tenant A do not block the same user/IP on Tenant B")
        void tenantSeparation_independentBuckets() {
            SecurityProperties props = createProperties(3, Duration.ofMinutes(10));
            LoginRateLimiter limiter = new LoginRateLimiter(props);
            String keyA = key(TENANT_A, IP_1, USERNAME);
            String keyB = key(TENANT_B, IP_1, USERNAME);

            // Exhaust attempts on Tenant A
            limiter.recordFailure(keyA);
            limiter.recordFailure(keyA);
            limiter.recordFailure(keyA);

            assertThatThrownBy(() -> limiter.checkAllowed(keyA))
                    .isInstanceOf(LoginRateLimitExceededException.class);
            // Tenant B must not be affected
            limiter.checkAllowed(keyB);

            // Clear Tenant A should not clear Tenant B
            limiter.recordFailure(keyB);
            limiter.clear(keyA);

            limiter.checkAllowed(keyA);
            // Tenant B still has 1 recorded failure, 2 more will block it
            limiter.recordFailure(keyB);
            limiter.recordFailure(keyB);
            assertThatThrownBy(() -> limiter.checkAllowed(keyB))
                    .isInstanceOf(LoginRateLimitExceededException.class);
        }
    }

    @Nested
    @DisplayName("IP Spoofing & Isolation")
    class IpIsolationTests {

        @Test
        @DisplayName("Different client IPs are tracked independently, preventing IP spoof denial-of-service")
        void ipSeparation_independentBuckets() {
            SecurityProperties props = createProperties(3, Duration.ofMinutes(10));
            LoginRateLimiter limiter = new LoginRateLimiter(props);
            String keyIp1 = key(TENANT_A, IP_1, USERNAME);
            String keyIp2 = key(TENANT_A, IP_2, USERNAME);

            // Attacker from IP_1 fails repeatedly
            limiter.recordFailure(keyIp1);
            limiter.recordFailure(keyIp1);
            limiter.recordFailure(keyIp1);

            // IP_1 is blocked
            assertThatThrownBy(() -> limiter.checkAllowed(keyIp1))
                    .isInstanceOf(LoginRateLimitExceededException.class);

            // Legitimate principal from IP_2 is NOT blocked
            limiter.checkAllowed(keyIp2);
        }
    }

    @Nested
    @DisplayName("Time Window & Expiration")
    class ExpirationTests {

        @Test
        @DisplayName("Expired failures outside window are not counted toward block threshold")
        void expiredFailures_notCounted() {
            Instant t0 = Instant.parse("2026-10-18T10:00:00Z");
            MutableClock clock = new MutableClock(t0);

            SecurityProperties props = createProperties(3, Duration.ofMinutes(10));
            LoginRateLimiter limiter = new LoginRateLimiter(props, clock, 100);
            String k = key(TENANT_A, IP_1, USERNAME);

            // 2 failures at t0
            limiter.recordFailure(k);
            limiter.recordFailure(k);

            // Advance clock past 10 minutes window
            clock.advance(Duration.ofMinutes(11));

            // 1 more failure at t0 + 11m
            limiter.recordFailure(k);

            // Total active failures should only be 1 (the 2 previous expired)
            limiter.checkAllowed(k);
        }
    }

    @Nested
    @DisplayName("High Cardinality & Memory Bounds")
    class HighCardinalityTests {

        @Test
        @DisplayName("Map size is bounded by maxKeys and purgeExpiredKeys frees stale entries")
        void boundedCapacity_andPurge() {
            Instant t0 = Instant.parse("2026-10-18T10:00:00Z");
            MutableClock clock = new MutableClock(t0);

            int maxKeys = 5;
            SecurityProperties props = createProperties(3, Duration.ofMinutes(5));
            LoginRateLimiter limiter = new LoginRateLimiter(props, clock, maxKeys);

            // Add 5 distinct entries
            for (int i = 1; i <= 5; i++) {
                limiter.recordFailure(key(TENANT_A, "192.168.1." + i, "user" + i + "@school.edu"));
            }
            assertThat(limiter.size()).isEqualTo(5);

            // Advance clock past expiration
            clock.advance(Duration.ofMinutes(6));

            // Adding a 6th key triggers purgeExpiredKeys, cleaning expired ones
            String key6 = key(TENANT_A, "192.168.1.6", "user6@school.edu");
            limiter.recordFailure(key6);

            // Previous 5 were expired and purged; only user6 remains
            assertThat(limiter.size()).isEqualTo(1);
            limiter.checkAllowed(key6);
        }

        @Test
        @DisplayName("purgeExpiredKeys retains active unexpired keys")
        void purgeExpiredKeys_retainsActiveKeys() {
            Instant t0 = Instant.parse("2026-10-18T10:00:00Z");
            MutableClock clock = new MutableClock(t0);

            SecurityProperties props = createProperties(3, Duration.ofMinutes(5));
            LoginRateLimiter limiter = new LoginRateLimiter(props, clock, 10);

            // Stale entry
            String staleKey = key(TENANT_A, IP_1, "stale@school.edu");
            limiter.recordFailure(staleKey);

            // Advance 4 minutes
            clock.advance(Duration.ofMinutes(4));

            // Fresh entry
            String freshKey = key(TENANT_A, IP_2, "fresh@school.edu");
            limiter.recordFailure(freshKey);

            // Advance 2 more minutes (stale is at 6m > 5m; fresh is at 2m < 5m)
            clock.advance(Duration.ofMinutes(2));

            limiter.purgeExpiredKeys();

            assertThat(limiter.size()).isEqualTo(1);
            limiter.checkAllowed(freshKey);
        }
    }

    private static class MutableClock extends Clock {
        private Instant current;

        MutableClock(Instant initial) {
            this.current = initial;
        }

        void advance(Duration duration) {
            this.current = this.current.plus(duration);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
