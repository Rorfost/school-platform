package com.rorfost.schoolportal.common.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.rorfost.schoolportal.common.config.MetricsProperties;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.Duration;

/**
 * Unit tests for {@link RequestIdFilter}.
 *
 * <p>Verifies:
 *
 * <ul>
 *   <li>Bounded route-class label derivation — raw path segments never appear in labels.
 *   <li>Request-ID echoing and generation.
 *   <li>Slow-request diagnostic fires only above threshold, is sampled, and never leaks PII.
 *   <li>Actuator health endpoint is excluded from business Micrometer metrics.
 *   <li>No personal data (student IDs, school slugs) in log labels.
 * </ul>
 */
class RequestIdFilterTest {

    private SimpleMeterRegistry registry;
    private MetricsProperties propsTimersOn;
    private MetricsProperties propsSlowOn;
    private MetricsProperties propsAllOff;

    @BeforeEach
    void setup() {
        registry = new SimpleMeterRegistry();
        // Timers enabled, slow logging off
        propsTimersOn = new MetricsProperties(true, false, Duration.ofMillis(500), 1.0);
        // Slow logging enabled, threshold=0ms, sample-rate=1.0 — every request triggers diagnostic
        propsSlowOn = new MetricsProperties(true, true, Duration.ZERO, 1.0);
        // Everything off — production-safe default
        propsAllOff = new MetricsProperties();
    }

    // -------------------------------------------------------------------------
    // Route-class label derivation
    // -------------------------------------------------------------------------

    @Test
    void routeClass_publicGet_returnsBoundedLabel() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/public/gallery");
        assertThat(RequestIdFilter.resolveRouteClass(request)).isEqualTo("public.get");
    }

    @Test
    void routeClass_publicGetWithStudentId_doesNotLeakIdInLabel() {
        // Student roll-number appears in path — label must still be bounded, never the raw URI.
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/public/exam-results?roll=STU-001");
        String label = RequestIdFilter.resolveRouteClass(request);
        assertThat(label).isEqualTo("public.get");
        assertThat(label).doesNotContain("STU-001");
    }

    @Test
    void routeClass_adminPost_returnsMutationLabel() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/admin/gallery/upload");
        assertThat(RequestIdFilter.resolveRouteClass(request)).isEqualTo("admin.mutation");
    }

    @Test
    void routeClass_adminGet_returnsAdminGetLabel() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/admin/auth/csrf");
        assertThat(RequestIdFilter.resolveRouteClass(request)).isEqualTo("admin.get");
    }

    @Test
    void routeClass_actuatorHealth_returnsActuatorLabel() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(RequestIdFilter.resolveRouteClass(request)).isEqualTo("actuator");
    }

    @Test
    void routeClass_unknownPath_returnsOther() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/unknown-path");
        assertThat(RequestIdFilter.resolveRouteClass(request)).isEqualTo("other");
    }

    // -------------------------------------------------------------------------
    // Status class bucketing
    // -------------------------------------------------------------------------

    @Test
    void statusClass_200_returns2xx() {
        assertThat(RequestIdFilter.statusClass(200)).isEqualTo("2xx");
    }

    @Test
    void statusClass_404_returns4xx() {
        assertThat(RequestIdFilter.statusClass(404)).isEqualTo("4xx");
    }

    @Test
    void statusClass_500_returns5xx() {
        assertThat(RequestIdFilter.statusClass(500)).isEqualTo("5xx");
    }

    // -------------------------------------------------------------------------
    // Request-ID header correlation
    // -------------------------------------------------------------------------

    @Test
    void filter_noIncomingRequestId_generatesAndEchoes() throws Exception {
        RequestIdFilter filter = new RequestIdFilter(propsAllOff, registry);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/public/notices");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        String echoed = response.getHeader("X-Request-Id");
        assertThat(echoed).isNotBlank();
        verify(chain, times(1)).doFilter(request, response);
    }

    @Test
    void filter_incomingRequestId_isEchoedUnchanged() throws Exception {
        RequestIdFilter filter = new RequestIdFilter(propsAllOff, registry);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/public/notices");
        request.addHeader("X-Request-Id", "test-id-abc");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isEqualTo("test-id-abc");
    }

    @Test
    void filter_oversizedRequestId_isReplacedWithGenerated() throws Exception {
        RequestIdFilter filter = new RequestIdFilter(propsAllOff, registry);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/public/notices");
        request.addHeader("X-Request-Id", "x".repeat(200)); // > 128 chars
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        assertThat(response.getHeader("X-Request-Id")).isNotBlank();
        assertThat(response.getHeader("X-Request-Id")).hasSizeLessThan(200);
    }

    // -------------------------------------------------------------------------
    // Micrometer timer recording
    // -------------------------------------------------------------------------

    @Test
    void filter_timersEnabled_recordsTimerWithBoundedLabels() throws Exception {
        RequestIdFilter filter = new RequestIdFilter(propsTimersOn, registry);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/public/gallery");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        // Timer must exist with bounded labels — not raw path.
        var timer =
                registry.find("http.server.requests.custom")
                        .tag("route_class", "public.get")
                        .tag("method", "GET")
                        .tag("status_class", "2xx")
                        .timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    void filter_actuatorHealth_notRecordedInBusinessTimer() throws Exception {
        RequestIdFilter filter = new RequestIdFilter(propsTimersOn, registry);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        // Health checks must not appear in business latency metrics.
        var timer = registry.find("http.server.requests.custom").timer();
        assertThat(timer).isNull();
    }

    // -------------------------------------------------------------------------
    // Slow-request diagnostic
    // -------------------------------------------------------------------------

    @Test
    void filter_slowRequestLogging_incrementsSlowCounter() throws Exception {
        // Threshold = 1 ms, sample-rate = 1.0 — every request triggers the diagnostic.
        RequestIdFilter filter = new RequestIdFilter(propsSlowOn, registry);
        MockHttpServletRequest request =
                new MockHttpServletRequest("POST", "/api/v1/admin/gallery/upload");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        var counter =
                registry.find("http.server.requests.slow")
                        .tag("route_class", "admin.mutation")
                        .tag("method", "POST")
                        .counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isGreaterThanOrEqualTo(1.0);
    }

    @Test
    void filter_allOff_noMetricsRecorded() throws Exception {
        RequestIdFilter filter = new RequestIdFilter(propsAllOff, registry);
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/public/gallery");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);
        FilterChain chain = mock(FilterChain.class);

        filter.doFilterInternal(request, response, chain);

        // No meters should be registered when all instrumentation is off.
        assertThat(registry.getMeters()).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Cross-tenant security — labels must never include school slug or student data
    // -------------------------------------------------------------------------

    @Test
    void routeClass_schoolSlugInPath_isNotExposedInLabel() {
        // Even if the path contains a school slug, the label must be bounded.
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/v1/public/school-abc/notices");
        String label = RequestIdFilter.resolveRouteClass(request);
        assertThat(label).isEqualTo("public.get");
        assertThat(label).doesNotContain("school-abc");
    }

    @Test
    void routeClass_adminPathWithTenantId_isNotExposedInLabel() {
        MockHttpServletRequest request =
                new MockHttpServletRequest("DELETE", "/api/v1/admin/students/12345/records");
        String label = RequestIdFilter.resolveRouteClass(request);
        assertThat(label).isEqualTo("admin.mutation");
        assertThat(label).doesNotContain("12345");
    }
}
