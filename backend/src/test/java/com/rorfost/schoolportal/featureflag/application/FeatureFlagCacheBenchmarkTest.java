package com.rorfost.schoolportal.featureflag.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.featureflag.domain.FeatureKey;
import com.rorfost.schoolportal.featureflag.domain.SchoolFeature;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;

import org.junit.jupiter.api.Test;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/** Repeatable local comparison of the feature snapshot cache with its uncached service path. */
class FeatureFlagCacheBenchmarkTest {
    private static final int WARMUP_OPERATIONS = 5_000;
    private static final int MEASURED_OPERATIONS = 20_000;
    private static final int ROUNDS = 3;

    @Test
    void comparesWarmCacheAndUncachedSyntheticFeatureReads() {
        SchoolFeatureRepository repository = mock(SchoolFeatureRepository.class);
        UUID schoolId = UUID.randomUUID();
        AtomicInteger repositoryReads = new AtomicInteger();
        List<SchoolFeature> overrides =
                List.of(
                        new SchoolFeature(schoolId, FeatureKey.GALLERY, false),
                        new SchoolFeature(schoolId, FeatureKey.TIMETABLE, true),
                        new SchoolFeature(schoolId, FeatureKey.ATTENDANCE, true));
        when(repository.findByIdSchoolId(schoolId))
                .thenAnswer(
                        invocation -> {
                            repositoryReads.incrementAndGet();
                            return overrides;
                        });

        FeatureFlagService uncachedService = new FeatureFlagService(repository);
        FeatureFlagService cachedService =
                new FeatureFlagService(
                        repository,
                        null,
                        null,
                        new FeatureFlagSnapshotCache(true, Duration.ofSeconds(30), 128));
        Runnable uncachedRead = () -> uncachedService.getFeaturesForSchool(schoolId);
        Runnable cachedRead = () -> cachedService.getFeaturesForSchool(schoolId);
        List<RoundResult> results = new ArrayList<>();

        for (int round = 1; round <= ROUNDS; round++) {
            repositoryReads.set(0);
            Metrics uncached = measure(uncachedRead, repositoryReads);
            int uncachedRepositoryReads = repositoryReads.get();

            repositoryReads.set(0);
            Metrics cached = measure(cachedRead, repositoryReads);
            int cachedRepositoryReads = repositoryReads.get();

            assertThat(uncachedRepositoryReads).isEqualTo(MEASURED_OPERATIONS);
            assertThat(cachedRepositoryReads).isZero();
            results.add(
                    new RoundResult(
                            round,
                            uncached,
                            cached,
                            uncachedRepositoryReads,
                            cachedRepositoryReads));
        }

        System.out.println("Sprint 40 feature snapshot synthetic benchmark (JVM process only)");
        System.out.println(
                "round,uncached_p50_us,uncached_p95_us,uncached_bytes_op,uncached_gc_count,uncached_gc_ms,uncached_repo_reads"
                        + ",cached_p50_us,cached_p95_us,cached_bytes_op,cached_gc_count,cached_gc_ms,cached_repo_reads");
        results.forEach(RoundResult::print);
        assertThat(results).hasSize(ROUNDS);
    }

    private static Metrics measure(Runnable operation, AtomicInteger repositoryReads) {
        for (int index = 0; index < WARMUP_OPERATIONS; index++) {
            operation.run();
        }
        repositoryReads.set(0);

        com.sun.management.ThreadMXBean threadBean =
                (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        if (threadBean.isThreadAllocatedMemorySupported()
                && !threadBean.isThreadAllocatedMemoryEnabled()) {
            threadBean.setThreadAllocatedMemoryEnabled(true);
        }

        long threadId = Thread.currentThread().threadId();
        long allocatedBefore =
                threadBean.isThreadAllocatedMemorySupported()
                        ? threadBean.getThreadAllocatedBytes(threadId)
                        : -1;
        long[] gcBefore = gcTotals();
        long[] elapsedNanos = new long[MEASURED_OPERATIONS];

        for (int index = 0; index < MEASURED_OPERATIONS; index++) {
            long start = System.nanoTime();
            operation.run();
            elapsedNanos[index] = System.nanoTime() - start;
        }

        long[] gcAfter = gcTotals();
        long allocatedAfter =
                threadBean.isThreadAllocatedMemorySupported()
                        ? threadBean.getThreadAllocatedBytes(threadId)
                        : -1;
        java.util.Arrays.sort(elapsedNanos);
        double allocatedBytesPerOperation =
                allocatedBefore < 0 || allocatedAfter < 0
                        ? Double.NaN
                        : (double) (allocatedAfter - allocatedBefore) / MEASURED_OPERATIONS;

        return new Metrics(
                percentileMicros(elapsedNanos, 0.50),
                percentileMicros(elapsedNanos, 0.95),
                allocatedBytesPerOperation,
                gcAfter[0] - gcBefore[0],
                gcAfter[1] - gcBefore[1]);
    }

    private static long[] gcTotals() {
        long collections = 0;
        long collectionMillis = 0;
        for (GarbageCollectorMXBean collector : ManagementFactory.getGarbageCollectorMXBeans()) {
            if (collector.getCollectionCount() >= 0) {
                collections += collector.getCollectionCount();
            }
            if (collector.getCollectionTime() >= 0) {
                collectionMillis += collector.getCollectionTime();
            }
        }
        return new long[] {collections, collectionMillis};
    }

    private static double percentileMicros(long[] sortedNanos, double percentile) {
        int index = (int) Math.ceil(percentile * sortedNanos.length) - 1;
        return sortedNanos[Math.max(0, index)] / 1_000.0;
    }

    private record Metrics(
            double p50Micros,
            double p95Micros,
            double allocatedBytesPerOperation,
            long gcCollections,
            long gcCollectionMillis) {}

    private record RoundResult(
            int round,
            Metrics uncached,
            Metrics cached,
            int uncachedRepositoryReads,
            int cachedRepositoryReads) {
        private void print() {
            System.out.printf(
                    java.util.Locale.ROOT,
                    "%d,%.3f,%.3f,%.1f,%d,%d,%d,%.3f,%.3f,%.1f,%d,%d,%d%n",
                    round,
                    uncached.p50Micros(),
                    uncached.p95Micros(),
                    uncached.allocatedBytesPerOperation(),
                    uncached.gcCollections(),
                    uncached.gcCollectionMillis(),
                    uncachedRepositoryReads,
                    cached.p50Micros(),
                    cached.p95Micros(),
                    cached.allocatedBytesPerOperation(),
                    cached.gcCollections(),
                    cached.gcCollectionMillis(),
                    cachedRepositoryReads);
        }
    }
}
