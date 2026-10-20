package com.rorfost.schoolportal.content.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.rorfost.schoolportal.common.security.SensitiveResponseHeadersFilter;
import com.rorfost.schoolportal.content.application.ContentService;
import com.rorfost.schoolportal.featureflag.application.FeatureAccessPolicy;
import com.rorfost.schoolportal.featureflag.application.FeatureFlagService;
import com.rorfost.schoolportal.featureflag.repository.SchoolFeatureRepository;
import com.rorfost.schoolportal.school.api.SchoolResponse;
import com.rorfost.schoolportal.school.application.SchoolService;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.request.RequestContextHolder;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

/**
 * Sprint 38: HTTP Cache-Control and Conditional Validation (ETag / 304) contract tests.
 *
 * <p>Validates:
 *
 * <ul>
 *   <li>Public semi-static endpoints return Cache-Control: max-age=300, s-maxage=300, public
 *   <li>Public dynamic endpoints return Cache-Control: max-age=60, s-maxage=60, public
 *   <li>ETag header is emitted and includes the verified tenant schoolId
 *   <li>Conditional GET with If-None-Match matching ETag returns 304 Not Modified
 *   <li>Cross-tenant isolation: School A's ETag submitted against School B returns 200 OK with
 *       School B's data
 *   <li>Visits counter and exam results carry Cache-Control: no-store and Pragma: no-cache
 * </ul>
 */
class HttpCacheHeadersContractTest {

    private MockMvc mockMvc;
    private SchoolService schoolService;
    private SchoolRepository schoolRepository;
    private ContentService contentService;
    private SensitiveResponseHeadersFilter sensitiveHeadersFilter;

    private UUID schoolIdA;
    private UUID schoolIdB;
    private School schoolA;
    private School schoolB;

    @BeforeEach
    void setUp() {
        schoolIdA = UUID.randomUUID();
        schoolIdB = UUID.randomUUID();

        schoolA = mock(School.class);
        when(schoolA.getId()).thenReturn(schoolIdA);
        schoolB = mock(School.class);
        when(schoolB.getId()).thenReturn(schoolIdB);

        schoolRepository = mock(SchoolRepository.class);
        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdA)).thenReturn(Optional.of(schoolA));
        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdB)).thenReturn(Optional.of(schoolB));

        schoolService = mock(SchoolService.class);
        contentService = mock(ContentService.class);

        SchoolFeatureRepository sfRepo = mock(SchoolFeatureRepository.class);
        when(sfRepo.findByIdSchoolId(any())).thenReturn(Collections.emptyList());
        FeatureAccessPolicy featureAccessPolicy =
                new FeatureAccessPolicy(new FeatureFlagService(sfRepo));

        sensitiveHeadersFilter = new SensitiveResponseHeadersFilter();

        com.rorfost.schoolportal.school.api.SchoolController schoolController =
                new com.rorfost.schoolportal.school.api.SchoolController(schoolService);

        com.rorfost.schoolportal.content.api.PublicContentController contentController =
                new com.rorfost.schoolportal.content.api.PublicContentController(
                        contentService, schoolRepository, featureAccessPolicy);

        mockMvc =
                MockMvcBuilders.standaloneSetup(schoolController, contentController)
                        .addFilters(sensitiveHeadersFilter)
                        .build();
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private RequestPostProcessor withTenant(UUID schoolId, String slug) {
        return request -> {
            TenantContextHolder.setTenantContext(request, new TenantContext(schoolId, slug));
            return request;
        };
    }

    @Test
    @DisplayName("GET /api/v1/public/school returns 300s public cache and ETag")
    void publicSchool_returnsCacheHeadersAndETag() throws Exception {
        SchoolResponse responseA =
                new SchoolResponse(
                        schoolIdA,
                        "School Alpha",
                        "Alpha",
                        "SCH001",
                        "Address",
                        "City",
                        "Taluka",
                        "District",
                        "State",
                        "380001",
                        "alpha@test.org",
                        "9876543210",
                        "https://alpha.org",
                        null,
                        "About Alpha",
                        "2000",
                        "Gujarati",
                        "Primary",
                        null,
                        null);

        when(schoolService.getPublic(schoolIdA)).thenReturn(responseA);

        MvcResult result =
                mockMvc.perform(
                                get("/api/v1/public/school")
                                        .with(withTenant(schoolIdA, "school-alpha")))
                        .andExpect(status().isOk())
                        .andExpect(
                                header().string(
                                                "Cache-Control",
                                                "max-age=300, public, s-maxage=300"))
                        .andExpect(header().exists("ETag"))
                        .andReturn();

        String etag = result.getResponse().getHeader("ETag");
        assertThat(etag).isNotBlank();
        assertThat(etag).contains(schoolIdA.toString());

        // Conditional GET with matching If-None-Match must return 304 Not Modified
        mockMvc.perform(
                        get("/api/v1/public/school")
                                .with(withTenant(schoolIdA, "school-alpha"))
                                .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    @DisplayName("Cross-tenant ETag validation: School A's ETag does not match School B")
    void crossTenant_etagDoesNotMatchAcrossSchools() throws Exception {
        SchoolResponse responseA =
                new SchoolResponse(
                        schoolIdA,
                        "School Alpha",
                        "Alpha",
                        "SCH001",
                        "Address",
                        "City",
                        "Taluka",
                        "District",
                        "State",
                        "380001",
                        "alpha@test.org",
                        "9876543210",
                        "https://alpha.org",
                        null,
                        "About Alpha",
                        "2000",
                        "Gujarati",
                        "Primary",
                        null,
                        null);

        SchoolResponse responseB =
                new SchoolResponse(
                        schoolIdB,
                        "School Beta",
                        "Beta",
                        "SCH002",
                        "Address B",
                        "City B",
                        "Taluka B",
                        "District B",
                        "State",
                        "380002",
                        "beta@test.org",
                        "9876543211",
                        "https://beta.org",
                        null,
                        "About Beta",
                        "2005",
                        "Gujarati",
                        "Primary",
                        null,
                        null);

        when(schoolService.getPublic(schoolIdA)).thenReturn(responseA);
        when(schoolService.getPublic(schoolIdB)).thenReturn(responseB);

        // Fetch School A ETag
        MvcResult resultA =
                mockMvc.perform(
                                get("/api/v1/public/school")
                                        .with(withTenant(schoolIdA, "school-alpha")))
                        .andExpect(status().isOk())
                        .andReturn();
        String etagA = resultA.getResponse().getHeader("ETag");

        // Request School B passing School A's ETag: must return 200 OK (not 304), with School B's
        // ETag
        MvcResult resultB =
                mockMvc.perform(
                                get("/api/v1/public/school")
                                        .with(withTenant(schoolIdB, "school-beta"))
                                        .header("If-None-Match", etagA))
                        .andExpect(status().isOk())
                        .andExpect(
                                header().string(
                                                "Cache-Control",
                                                "max-age=300, public, s-maxage=300"))
                        .andReturn();

        String etagB = resultB.getResponse().getHeader("ETag");
        assertThat(etagB).isNotEqualTo(etagA);
        assertThat(etagB).contains(schoolIdB.toString());
    }

    @Test
    @DisplayName(
            "GET /api/v1/public/materials returns 60s public cache and ETag conditional validation")
    void publicMaterials_returnsCacheHeadersAnd304() throws Exception {
        when(contentService.publicMaterialPage(schoolIdA, 0, 20))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        MvcResult result =
                mockMvc.perform(
                                get("/api/v1/public/materials")
                                        .with(withTenant(schoolIdA, "school-alpha")))
                        .andExpect(status().isOk())
                        .andExpect(
                                header().string("Cache-Control", "max-age=60, public, s-maxage=60"))
                        .andExpect(header().exists("ETag"))
                        .andReturn();

        String etag = result.getResponse().getHeader("ETag");
        assertThat(etag).contains(schoolIdA.toString());

        // Conditional GET with matching If-None-Match returns 304
        mockMvc.perform(
                        get("/api/v1/public/materials")
                                .with(withTenant(schoolIdA, "school-alpha"))
                                .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    @DisplayName(
            "GET /api/v1/public/notices returns 60s public cache and ETag conditional validation")
    void publicNotices_returnsCacheHeadersAnd304() throws Exception {
        when(contentService.publicNotices(schoolIdA, 0, 20))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0));

        MvcResult result =
                mockMvc.perform(
                                get("/api/v1/public/notices")
                                        .with(withTenant(schoolIdA, "school-alpha")))
                        .andExpect(status().isOk())
                        .andExpect(
                                header().string("Cache-Control", "max-age=60, public, s-maxage=60"))
                        .andExpect(header().exists("ETag"))
                        .andReturn();

        String etag = result.getResponse().getHeader("ETag");
        assertThat(etag).contains(schoolIdA.toString());

        mockMvc.perform(
                        get("/api/v1/public/notices")
                                .with(withTenant(schoolIdA, "school-alpha"))
                                .header("If-None-Match", etag))
                .andExpect(status().isNotModified());
    }

    @Test
    @DisplayName("SensitiveResponseHeadersFilter enforces no-store on visits counter and mutations")
    void sensitiveFilter_enforcesNoStoreOnVisitsAndMutations() throws Exception {
        mockMvc.perform(get("/api/v1/public/visits"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"));

        mockMvc.perform(post("/api/v1/public/visits"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Pragma", "no-cache"));
    }
}
