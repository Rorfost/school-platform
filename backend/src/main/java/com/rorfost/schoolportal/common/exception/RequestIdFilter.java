package com.rorfost.schoolportal.common.exception;

import com.rorfost.schoolportal.common.config.MetricsProperties;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Servlet filter that:
 *
 * <ol>
 *   <li>Assigns or echoes a {@code X-Request-Id} header for end-to-end log correlation.
 *   <li>Emits a structured INFO log line after every request (method, route-class, status-class,
 *       duration). Raw URI path segments are replaced with a bounded route-class label to prevent
 *       high-cardinality and to avoid leaking student IDs, school slugs or other sensitive URL
 *       parameters into log lines.
 *   <li>Optionally records anonymous Micrometer counters/timers (opt-in via {@code
 *       app.metrics.timers-enabled}). Labels are bounded: {@code route_class}, {@code method},
 *       {@code status_class}. Health-check traffic is excluded from business metrics.
 *   <li>Optionally logs a WARN diagnostic for slow requests (opt-in via {@code
 *       app.metrics.slow-request-logging-enabled}) with request-ID correlation and no personal data
 *       in the message.
 * </ol>
 *
 * <p>No metrics scraping endpoint is exposed to the public; the Actuator is locked to {@code
 * /actuator/health} only. See {@code docs/operations/PERFORMANCE_METRICS.md} for sampling guidance.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Request-Id";
    private static final Logger log = LoggerFactory.getLogger(RequestIdFilter.class);

    // Micrometer metric names — stable, never change without a migration plan.
    private static final String METRIC_REQUESTS = "http.server.requests.custom";
    private static final String METRIC_SLOW = "http.server.requests.slow";

    private final MetricsProperties metricsProps;
    private final MeterRegistry meterRegistry;

    // Intern-style cache for Timer/Counter lookups — avoids repeated map lookups per request.
    // Key: "routeClass|method|statusClass", Value: pre-built Timer.
    private final Map<String, Timer> timerCache = new ConcurrentHashMap<>(64);
    private final Map<String, Counter> slowCounterCache = new ConcurrentHashMap<>(16);

    public RequestIdFilter(MetricsProperties metricsProps, MeterRegistry meterRegistry) {
        this.metricsProps = metricsProps;
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestId = request.getHeader(HEADER_NAME);
        if (requestId == null || requestId.isBlank() || requestId.length() > 128) {
            requestId = UUID.randomUUID().toString();
        }

        long startNanos = System.nanoTime();
        MDC.put("requestId", requestId);
        response.setHeader(HEADER_NAME, requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            long durationMillis = TimeUnit.NANOSECONDS.toMillis(durationNanos);

            // Derive bounded route-class label — never log raw URI to prevent data leakage.
            String routeClass = resolveRouteClass(request);
            String method = request.getMethod();
            String statusClass = statusClass(response.getStatus());
            boolean isHealthCheck = "/actuator/health".equals(request.getRequestURI());

            log.info(
                    "request_complete requestId={} method={} routeClass={} statusClass={}"
                            + " status={} durationMs={}",
                    requestId,
                    method,
                    routeClass,
                    statusClass,
                    response.getStatus(),
                    durationMillis);

            // Record Micrometer timer — skip health checks to keep business latency clean.
            if (metricsProps.timersEnabled() && !isHealthCheck) {
                recordTimer(routeClass, method, statusClass, durationNanos);
            }

            // Emit slow-request WARN with request-ID correlation; sample to bound log volume.
            if (metricsProps.slowRequestLoggingEnabled()
                    && !isHealthCheck
                    && durationMillis >= metricsProps.slowRequestThreshold().toMillis()
                    && ThreadLocalRandom.current().nextDouble()
                            < metricsProps.slowRequestSampleRate()) {
                log.warn(
                        "slow_request requestId={} method={} routeClass={} statusClass={}"
                                + " durationMs={} thresholdMs={}",
                        requestId,
                        method,
                        routeClass,
                        statusClass,
                        durationMillis,
                        metricsProps.slowRequestThreshold().toMillis());

                // Increment a slow-request counter for coarse trending (no external scraper needed
                // — visible via /actuator/metrics if Micrometer is on classpath).
                if (metricsProps.timersEnabled()) {
                    slowCounter(routeClass, method).increment();
                }
            }

            MDC.remove("requestId");
        }
    }

    // ---------------------------------------------------------------------------
    // Private helpers
    // ---------------------------------------------------------------------------

    /**
     * Maps the incoming request to a bounded route-class label suitable for metric tags and
     * structured log fields. Never exposes student IDs, roll numbers, school slugs, UUIDs, or any
     * URL-encoded user content.
     *
     * <p>Label vocabulary is deliberately small: {@code public.get}, {@code admin.mutation}, {@code
     * admin.get}, {@code actuator}, {@code other}. This prevents cardinality explosion which would
     * exhaust Micrometer's in-process tag space on Render Free instances.
     */
    static String resolveRouteClass(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri == null) {
            return "other";
        }

        if (uri.startsWith("/actuator")) {
            return "actuator";
        }

        if (uri.startsWith("/api/v1/public/")) {
            // Public reads are the main cacheable traffic; separate from admin.
            return "GET".equalsIgnoreCase(method) ? "public.get" : "public.mutation";
        }

        if (uri.startsWith("/api/v1/admin/")) {
            return "GET".equalsIgnoreCase(method) ? "admin.get" : "admin.mutation";
        }

        return "other";
    }

    /**
     * Collapses HTTP status into a 3-class label ({@code 2xx}, {@code 4xx}, {@code 5xx}, or {@code
     * other}) to keep metric cardinality bounded.
     */
    static String statusClass(int status) {
        if (status >= 200 && status < 300) return "2xx";
        if (status >= 300 && status < 400) return "3xx";
        if (status >= 400 && status < 500) return "4xx";
        if (status >= 500) return "5xx";
        return "other";
    }

    private void recordTimer(
            String routeClass, String method, String statusClass, long durationNanos) {
        String key = routeClass + "|" + method + "|" + statusClass;
        Timer timer =
                timerCache.computeIfAbsent(
                        key,
                        k ->
                                Timer.builder(METRIC_REQUESTS)
                                        .description(
                                                "Anonymous backend request latency with bounded labels")
                                        .tag("route_class", routeClass)
                                        .tag("method", method)
                                        .tag("status_class", statusClass)
                                        .register(meterRegistry));
        timer.record(durationNanos, TimeUnit.NANOSECONDS);
    }

    private Counter slowCounter(String routeClass, String method) {
        String key = routeClass + "|" + method;
        return slowCounterCache.computeIfAbsent(
                key,
                k ->
                        Counter.builder(METRIC_SLOW)
                                .description("Count of sampled slow requests exceeding threshold")
                                .tag("route_class", routeClass)
                                .tag("method", method)
                                .register(meterRegistry));
    }
}
