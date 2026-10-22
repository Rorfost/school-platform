package com.rorfost.schoolportal.featureflag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class FeatureFlagSnapshotCacheTest {

    @Test
    void cachesPerSchoolAndDoesNotMixTenantSnapshots() {
        FeatureFlagSnapshotCache cache =
                new FeatureFlagSnapshotCache(true, Duration.ofSeconds(1), 4);
        UUID schoolA = UUID.randomUUID();
        UUID schoolB = UUID.randomUUID();
        AtomicInteger schoolAReads = new AtomicInteger();
        AtomicInteger schoolBReads = new AtomicInteger();

        Map<String, Boolean> schoolAFlags =
                cache.get(
                        schoolA,
                        () -> {
                            schoolAReads.incrementAndGet();
                            return Map.of("GALLERY", true);
                        });
        Map<String, Boolean> repeatedSchoolAFlags =
                cache.get(
                        schoolA,
                        () -> {
                            schoolAReads.incrementAndGet();
                            return Map.of("GALLERY", false);
                        });
        Map<String, Boolean> schoolBFlags =
                cache.get(
                        schoolB,
                        () -> {
                            schoolBReads.incrementAndGet();
                            return Map.of("GALLERY", false);
                        });

        assertThat(repeatedSchoolAFlags).isSameAs(schoolAFlags);
        assertThat(schoolAFlags.get("GALLERY")).isTrue();
        assertThat(schoolBFlags.get("GALLERY")).isFalse();
        assertThat(schoolAReads).hasValue(1);
        assertThat(schoolBReads).hasValue(1);
    }

    @Test
    void expiresEntriesAndBoundsLeastRecentlyUsedSchools() throws InterruptedException {
        FeatureFlagSnapshotCache expiring =
                new FeatureFlagSnapshotCache(true, Duration.ofMillis(20), 1);
        UUID schoolA = UUID.randomUUID();
        AtomicInteger reads = new AtomicInteger();
        expiring.get(schoolA, () -> Map.of("GALLERY", reads.incrementAndGet() % 2 == 1));
        Thread.sleep(30);
        expiring.get(schoolA, () -> Map.of("GALLERY", reads.incrementAndGet() % 2 == 1));
        assertThat(reads).hasValue(2);

        FeatureFlagSnapshotCache bounded =
                new FeatureFlagSnapshotCache(true, Duration.ofSeconds(1), 1);
        UUID schoolB = UUID.randomUUID();
        AtomicInteger schoolAReads = new AtomicInteger();
        bounded.get(schoolA, () -> Map.of("GALLERY", schoolAReads.incrementAndGet() > 0));
        bounded.get(schoolB, () -> Map.of("GALLERY", true));
        bounded.get(schoolA, () -> Map.of("GALLERY", schoolAReads.incrementAndGet() > 1));
        assertThat(schoolAReads).hasValue(2);
    }

    @Test
    void disabledCacheAlwaysLoadsAndDoesNotRetainEmptyOrFailedResults() {
        UUID schoolId = UUID.randomUUID();
        FeatureFlagSnapshotCache disabled =
                new FeatureFlagSnapshotCache(false, Duration.ofSeconds(1), 1);
        AtomicInteger disabledReads = new AtomicInteger();
        disabled.get(schoolId, () -> Map.of("GALLERY", disabledReads.incrementAndGet() > 0));
        disabled.get(schoolId, () -> Map.of("GALLERY", disabledReads.incrementAndGet() > 1));
        assertThat(disabledReads).hasValue(2);

        FeatureFlagSnapshotCache cache =
                new FeatureFlagSnapshotCache(true, Duration.ofSeconds(1), 1);
        assertThat(cache.get(schoolId, Map::of)).isEmpty();
        AtomicInteger successfulReads = new AtomicInteger();
        assertThatThrownBy(
                        () ->
                                cache.get(
                                        schoolId,
                                        () -> {
                                            throw new IllegalStateException("load failed");
                                        }))
                .isInstanceOf(IllegalStateException.class);
        cache.get(schoolId, () -> Map.of("GALLERY", successfulReads.incrementAndGet() > 0));
        cache.get(schoolId, () -> Map.of("GALLERY", successfulReads.incrementAndGet() > 1));
        assertThat(successfulReads).hasValue(1);
    }

    @Test
    void invalidationCannotBeFollowedByAStaleInFlightLoad() throws Exception {
        FeatureFlagSnapshotCache cache =
                new FeatureFlagSnapshotCache(true, Duration.ofSeconds(1), 4);
        UUID schoolId = UUID.randomUUID();
        CountDownLatch loaderStarted = new CountDownLatch(1);
        CountDownLatch releaseLoader = new CountDownLatch(1);
        AtomicInteger reads = new AtomicInteger();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<Map<String, Boolean>> firstRead =
                    executor.submit(
                            () ->
                                    cache.get(
                                            schoolId,
                                            () -> {
                                                reads.incrementAndGet();
                                                loaderStarted.countDown();
                                                await(releaseLoader);
                                                return Map.of("GALLERY", true);
                                            }));
            assertThat(loaderStarted.await(1, TimeUnit.SECONDS)).isTrue();

            Future<?> invalidation = executor.submit(() -> cache.invalidate(schoolId));
            Thread.sleep(30);
            assertThat(invalidation.isDone()).isFalse();

            releaseLoader.countDown();
            assertThat(firstRead.get(1, TimeUnit.SECONDS).get("GALLERY")).isTrue();
            invalidation.get(1, TimeUnit.SECONDS);
            assertThat(cache.get(schoolId, () -> Map.of("GALLERY", false)).get("GALLERY"))
                    .isFalse();
            assertThat(reads).hasValue(1);
        } finally {
            releaseLoader.countDown();
            executor.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(1, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for test latch");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for test latch", exception);
        }
    }
}
