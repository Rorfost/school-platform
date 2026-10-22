package com.rorfost.schoolportal.featureflag.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/** Bounded cache for the public feature snapshot; authorization checks remain uncached. */
@Component
public class FeatureFlagSnapshotCache {
    static final int MAX_ENTRIES = 128;
    static final Duration TTL = Duration.ofSeconds(30);

    private final boolean enabled;
    private final long ttlNanos;
    private final int maxEntries;
    private final Object[] schoolLocks = new Object[64];
    private final Map<UUID, Entry> entries = new LinkedHashMap<>(16, 0.75f, true);

    public FeatureFlagSnapshotCache(
            @Value("${app.cache.public-features.enabled:true}") boolean enabled) {
        this(enabled, TTL, MAX_ENTRIES);
    }

    FeatureFlagSnapshotCache(boolean enabled, Duration ttl, int maxEntries) {
        if (ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("ttl must be positive");
        }
        if (maxEntries < 1) {
            throw new IllegalArgumentException("maxEntries must be positive");
        }
        this.enabled = enabled;
        this.ttlNanos = ttl.toNanos();
        this.maxEntries = maxEntries;
        for (int index = 0; index < schoolLocks.length; index++) {
            schoolLocks[index] = new Object();
        }
    }

    public Map<String, Boolean> get(UUID schoolId, Supplier<Map<String, Boolean>> loader) {
        Objects.requireNonNull(schoolId, "schoolId must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        if (!enabled) {
            return loader.get();
        }

        synchronized (lockFor(schoolId)) {
            long now = System.nanoTime();
            synchronized (entries) {
                Entry entry = entries.get(schoolId);
                if (entry != null && now - entry.createdAtNanos < ttlNanos) {
                    return entry.snapshot;
                }
                if (entry != null) {
                    entries.remove(schoolId);
                }
            }

            // Per-school single-flight prevents a pre-commit read from repopulating after eviction.
            Map<String, Boolean> snapshot = Objects.requireNonNull(loader.get());
            if (!snapshot.isEmpty()) {
                synchronized (entries) {
                    entries.put(schoolId, new Entry(snapshot, System.nanoTime()));
                    while (entries.size() > maxEntries) {
                        UUID eldestSchoolId = entries.keySet().iterator().next();
                        entries.remove(eldestSchoolId);
                    }
                }
            }
            return snapshot;
        }
    }

    public void invalidate(UUID schoolId) {
        Objects.requireNonNull(schoolId, "schoolId must not be null");
        synchronized (lockFor(schoolId)) {
            synchronized (entries) {
                entries.remove(schoolId);
            }
        }
    }

    private Object lockFor(UUID schoolId) {
        return schoolLocks[(schoolId.hashCode() & Integer.MAX_VALUE) % schoolLocks.length];
    }

    private record Entry(Map<String, Boolean> snapshot, long createdAtNanos) {}
}
