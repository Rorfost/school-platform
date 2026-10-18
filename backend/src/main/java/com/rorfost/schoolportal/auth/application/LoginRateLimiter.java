package com.rorfost.schoolportal.auth.application;

import com.rorfost.schoolportal.common.config.SecurityProperties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory sliding-window rate limiter for login attempts.
 *
 * <p>Keys are tenant-scoped ({@code schoolId:clientIp:email}) to prevent cross-tenant lockout
 * interference. The map is bounded: when the number of tracked keys exceeds {@code maxKeys},
 * fully-expired entries are evicted synchronously on the calling thread. If the map is still at
 * capacity after eviction the new key is silently dropped (fail-open for availability; DOS on
 * rate-limiter state does not bypass the limit for existing tracked IPs).
 */
@Component
public class LoginRateLimiter {

    /**
     * Maximum number of distinct keys tracked in memory. Each key is at most {@code
     * loginMaxAttempts} {@link Instant} entries. At 48 bytes/Instant this is well under 1 MB for
     * the default cap.
     */
    static final int DEFAULT_MAX_KEYS = 10_000;

    private final ConcurrentHashMap<String, Deque<Instant>> failedAttempts =
            new ConcurrentHashMap<>();
    private final SecurityProperties properties;
    private final Clock clock;
    private final int maxKeys;

    @Autowired
    public LoginRateLimiter(SecurityProperties properties) {
        this(properties, Clock.systemUTC(), DEFAULT_MAX_KEYS);
    }

    LoginRateLimiter(SecurityProperties properties, Clock clock, int maxKeys) {
        this.properties = properties;
        this.clock = clock;
        this.maxKeys = maxKeys;
    }

    public void checkAllowed(String key) {
        Deque<Instant> attempts = failedAttempts.get(key);
        if (attempts == null) {
            return;
        }
        synchronized (attempts) {
            removeExpired(attempts);
            if (attempts.size() >= properties.loginMaxAttempts()) {
                throw new LoginRateLimitExceededException();
            }
            if (attempts.isEmpty()) {
                failedAttempts.remove(key, attempts);
            }
        }
    }

    public void recordFailure(String key) {
        // Evict expired keys if map is at capacity before inserting a new one.
        if (!failedAttempts.containsKey(key) && failedAttempts.size() >= maxKeys) {
            purgeExpiredKeys();
            // If still at capacity, drop the new key (fail-open for new IPs; existing tracked
            // IPs are unaffected and continue to be rate-limited normally).
            if (failedAttempts.size() >= maxKeys) {
                return;
            }
        }
        Deque<Instant> attempts =
                failedAttempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (attempts) {
            removeExpired(attempts);
            attempts.addLast(clock.instant());
        }
    }

    public void clear(String key) {
        failedAttempts.remove(key);
    }

    /**
     * Removes all keys whose deque is empty after expiring old entries. Called when the map
     * reaches {@code maxKeys} capacity to reclaim memory for new clients.
     */
    void purgeExpiredKeys() {
        List<String> toRemove = new ArrayList<>();
        for (var entry : failedAttempts.entrySet()) {
            Deque<Instant> deque = entry.getValue();
            synchronized (deque) {
                removeExpired(deque);
                if (deque.isEmpty()) {
                    toRemove.add(entry.getKey());
                }
            }
        }
        for (String key : toRemove) {
            // Use two-arg remove to only delete if the value hasn't been replaced
            Deque<Instant> deque = failedAttempts.get(key);
            if (deque != null) {
                synchronized (deque) {
                    if (deque.isEmpty()) {
                        failedAttempts.remove(key, deque);
                    }
                }
            }
        }
    }

    /** Visible for testing. */
    int size() {
        return failedAttempts.size();
    }

    private void removeExpired(Deque<Instant> attempts) {
        Instant threshold = clock.instant().minus(properties.loginAttemptWindow());
        while (!attempts.isEmpty() && !attempts.peekFirst().isAfter(threshold)) {
            attempts.removeFirst();
        }
    }
}
