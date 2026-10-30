package com.rorfost.schoolportal.academic.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rorfost.schoolportal.academic.application.AcademicConfigurationService;
import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.StandardClass;
import com.rorfost.schoolportal.academic.domain.StandardSubject;
import com.rorfost.schoolportal.academic.domain.Subject;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardClassRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.audit.service.AuditLogService;
import com.rorfost.schoolportal.common.exception.ApiExceptionHandler;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.common.storage.StorageService;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantResolver;
import com.rorfost.schoolportal.tenant.domain.TenantContext;
import com.rorfost.schoolportal.tenant.security.GatewayCredentialAuthenticationFilter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class PublicAcademicTenantIsolationTest {

    private static final String TOKEN_A = "worker-secret-school-a";
    private static final String TOKEN_B = "worker-secret-school-b";
    private static final String TOKEN_INACTIVE = "worker-secret-school-inactive";

    private AcademicYearRepository academicYearRepository;
    private StandardRepository standardRepository;
    private SubjectRepository subjectRepository;
    private StandardSubjectRepository standardSubjectRepository;
    private StandardClassRepository standardClassRepository;
    private SchoolRepository schoolRepository;
    private StorageService storageService;
    private TenantResolver tenantResolver;
    private AcademicConfigurationService academicService;
    private MockMvc mockMvc;

    private UUID schoolIdA;
    private UUID schoolIdB;
    private UUID schoolIdInactive;
    private School schoolA;
    private School schoolB;

    private UUID standardIdA;
    private UUID standardIdB;
    private Standard standardA;
    private Standard standardB;

    private UUID subjectIdA_Math;
    private UUID subjectIdA_Sci;
    private UUID subjectIdB_Math;
    private UUID subjectIdB_Eng;
    private Subject subjectA_Math;
    private Subject subjectA_Sci;
    private Subject subjectB_Math;
    private Subject subjectB_Eng;

    private StandardSubject mappingA_Math;
    private StandardSubject mappingA_Sci;
    private StandardSubject mappingB_Math;
    private StandardSubject mappingB_Eng;

    private UUID yearIdA;
    private UUID yearIdB;
    private AcademicYear yearA;
    private AcademicYear yearB;

    @BeforeEach
    void setUp() {
        academicYearRepository = mock(AcademicYearRepository.class);
        standardRepository = mock(StandardRepository.class);
        subjectRepository = mock(SubjectRepository.class);
        standardSubjectRepository = mock(StandardSubjectRepository.class);
        standardClassRepository = mock(StandardClassRepository.class);
        schoolRepository = mock(SchoolRepository.class);
        storageService = mock(StorageService.class);
        tenantResolver = mock(TenantResolver.class);
        AuditLogService auditLogService = mock(AuditLogService.class);
        ObjectMapper objectMapper = new ObjectMapper();

        academicService =
                new AcademicConfigurationService(
                        academicYearRepository,
                        standardRepository,
                        subjectRepository,
                        standardSubjectRepository,
                        standardClassRepository,
                        auditLogService,
                        storageService);

        PublicAcademicController controller =
                new PublicAcademicController(
                        academicService, schoolRepository, academicYearRepository);

        GatewayCredentialAuthenticationFilter gatewayFilter =
                new GatewayCredentialAuthenticationFilter(tenantResolver, objectMapper, true);

        mockMvc =
                MockMvcBuilders.standaloneSetup(controller)
                        .setControllerAdvice(new ApiExceptionHandler())
                        .addFilters(gatewayFilter)
                        .build();

        // Schools setup
        schoolIdA = UUID.randomUUID();
        schoolIdB = UUID.randomUUID();
        schoolIdInactive = UUID.randomUUID();

        schoolA = new School("School A Primary", "school-a");
        schoolB = new School("School B Academy", "school-b");
        ReflectionTestUtils.setField(schoolA, "id", schoolIdA);
        ReflectionTestUtils.setField(schoolB, "id", schoolIdB);

        // Tenant resolution setup
        when(tenantResolver.resolveTenant(TOKEN_A))
                .thenReturn(Optional.of(new TenantContext(schoolIdA, "school-a")));
        when(tenantResolver.resolveTenant(TOKEN_B))
                .thenReturn(Optional.of(new TenantContext(schoolIdB, "school-b")));
        when(tenantResolver.resolveTenant(TOKEN_INACTIVE)).thenReturn(Optional.empty());

        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdA)).thenReturn(Optional.of(schoolA));
        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdB)).thenReturn(Optional.of(schoolB));
        when(schoolRepository.findByIdAndIsActiveTrue(schoolIdInactive))
                .thenReturn(Optional.empty());

        // Storage mock
        when(storageService.publicUrl(any()))
                .thenAnswer(inv -> "https://ik.imagekit.io/" + inv.getArgument(0));

        // Standards setup: same code "STD_10" in both schools, different display names and classes
        standardIdA = UUID.randomUUID();
        standardA = new Standard(schoolIdA, "STD_10", "ધોરણ ૧૦ (School A)", (short) 1);
        standardA.updateClassTeacher("પટેલ રમેશભાઈ");
        standardA.changeClassTeacherSignature("signatures/" + schoolIdA + "/class-teacher.png");
        ReflectionTestUtils.setField(standardA, "id", standardIdA);

        standardIdB = UUID.randomUUID();
        standardB = new Standard(schoolIdB, "STD_10", "ધોરણ ૧૦ (School B)", (short) 1);
        standardB.updateClassTeacher("શાહ સુરેશભાઈ");
        ReflectionTestUtils.setField(standardB, "id", standardIdB);

        // Classes for Standard 10
        StandardClass classA1 = new StandardClass(schoolIdA, standardIdA, "૧૦-અ", (short) 1);
        StandardClass classA2 = new StandardClass(schoolIdA, standardIdA, "૧૦-બ", (short) 2);
        StandardClass classB1 = new StandardClass(schoolIdB, standardIdB, "૧૦-ક", (short) 1);
        StandardClass classB2 = new StandardClass(schoolIdB, standardIdB, "૧૦-ડ", (short) 2);

        // Subjects setup: same code "MATH" in both schools, different additional subjects
        subjectIdA_Math = UUID.randomUUID();
        subjectA_Math = new Subject(schoolIdA, "MATH", "ગણિત (School A)", (short) 1);
        ReflectionTestUtils.setField(subjectA_Math, "id", subjectIdA_Math);

        subjectIdA_Sci = UUID.randomUUID();
        subjectA_Sci = new Subject(schoolIdA, "SCI", "વિજ્ઞાન (School A)", (short) 2);
        ReflectionTestUtils.setField(subjectA_Sci, "id", subjectIdA_Sci);

        subjectIdB_Math = UUID.randomUUID();
        subjectB_Math = new Subject(schoolIdB, "MATH", "ગણિત (School B)", (short) 1);
        ReflectionTestUtils.setField(subjectB_Math, "id", subjectIdB_Math);

        subjectIdB_Eng = UUID.randomUUID();
        subjectB_Eng = new Subject(schoolIdB, "ENG", "અંગ્રેજી (School B)", (short) 2);
        ReflectionTestUtils.setField(subjectB_Eng, "id", subjectIdB_Eng);

        // Standard-Subject Mappings setup: different subject assignments and max marks
        mappingA_Math = new StandardSubject(schoolIdA, standardIdA, subjectIdA_Math, (short) 1);
        mappingA_Math.setMaximumMarks(100);
        ReflectionTestUtils.setField(mappingA_Math, "id", UUID.randomUUID());

        mappingA_Sci = new StandardSubject(schoolIdA, standardIdA, subjectIdA_Sci, (short) 2);
        mappingA_Sci.setMaximumMarks(80);
        ReflectionTestUtils.setField(mappingA_Sci, "id", UUID.randomUUID());

        mappingB_Math = new StandardSubject(schoolIdB, standardIdB, subjectIdB_Math, (short) 1);
        mappingB_Math.setMaximumMarks(50);
        ReflectionTestUtils.setField(mappingB_Math, "id", UUID.randomUUID());

        mappingB_Eng = new StandardSubject(schoolIdB, standardIdB, subjectIdB_Eng, (short) 2);
        mappingB_Eng.setMaximumMarks(75);
        ReflectionTestUtils.setField(mappingB_Eng, "id", UUID.randomUUID());

        // Academic Years setup
        yearIdA = UUID.randomUUID();
        yearA =
                new AcademicYear(
                        schoolIdA,
                        "૨૦૨૬-૨૦૨૭ (A)",
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2027, 4, 30),
                        AcademicYearStatus.CURRENT,
                        null);
        ReflectionTestUtils.setField(yearA, "id", yearIdA);

        yearIdB = UUID.randomUUID();
        yearB =
                new AcademicYear(
                        schoolIdB,
                        "૨૦૨૬-૨૦૨૭ (B)",
                        LocalDate.of(2026, 6, 15),
                        LocalDate.of(2027, 5, 15),
                        AcademicYearStatus.CURRENT,
                        null);
        ReflectionTestUtils.setField(yearB, "id", yearIdB);

        // Mock repository returns for School A
        when(standardRepository.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolIdA))
                .thenReturn(List.of(standardA));
        when(standardRepository.findByIdAndSchoolId(standardIdA, schoolIdA))
                .thenReturn(Optional.of(standardA));
        when(standardRepository.findByIdAndSchoolId(standardIdA, schoolIdB))
                .thenReturn(Optional.empty());
        when(standardClassRepository.findBySchoolIdAndStandardIdOrderBySortOrder(
                        schoolIdA, standardIdA))
                .thenReturn(List.of(classA1, classA2));

        when(subjectRepository.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolIdA))
                .thenReturn(List.of(subjectA_Math, subjectA_Sci));
        when(subjectRepository.findByIdAndSchoolId(subjectIdA_Math, schoolIdA))
                .thenReturn(Optional.of(subjectA_Math));
        when(subjectRepository.findByIdAndSchoolId(subjectIdA_Math, schoolIdB))
                .thenReturn(Optional.empty());
        when(subjectRepository.findByIdAndSchoolId(subjectIdA_Sci, schoolIdA))
                .thenReturn(Optional.of(subjectA_Sci));
        when(subjectRepository.findByIdAndSchoolId(subjectIdA_Sci, schoolIdB))
                .thenReturn(Optional.empty());

        when(standardSubjectRepository.findBySchoolIdAndStandardIdOrderBySortOrder(
                        schoolIdA, standardIdA))
                .thenReturn(List.of(mappingA_Math, mappingA_Sci));
        when(standardSubjectRepository.findByIdAndSchoolId(mappingA_Math.getId(), schoolIdA))
                .thenReturn(Optional.of(mappingA_Math));
        when(standardSubjectRepository.findByIdAndSchoolId(mappingA_Math.getId(), schoolIdB))
                .thenReturn(Optional.empty());

        when(academicYearRepository.findBySchoolIdAndStatus(schoolIdA, AcademicYearStatus.CURRENT))
                .thenReturn(Optional.of(yearA));
        when(academicYearRepository.findByIdAndSchoolId(yearIdA, schoolIdA))
                .thenReturn(Optional.of(yearA));
        when(academicYearRepository.findByIdAndSchoolId(yearIdA, schoolIdB))
                .thenReturn(Optional.empty());

        // Mock repository returns for School B
        when(standardRepository.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolIdB))
                .thenReturn(List.of(standardB));
        when(standardRepository.findByIdAndSchoolId(standardIdB, schoolIdB))
                .thenReturn(Optional.of(standardB));
        when(standardRepository.findByIdAndSchoolId(standardIdB, schoolIdA))
                .thenReturn(Optional.empty());
        when(standardClassRepository.findBySchoolIdAndStandardIdOrderBySortOrder(
                        schoolIdB, standardIdB))
                .thenReturn(List.of(classB1, classB2));

        when(subjectRepository.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolIdB))
                .thenReturn(List.of(subjectB_Math, subjectB_Eng));
        when(subjectRepository.findByIdAndSchoolId(subjectIdB_Math, schoolIdB))
                .thenReturn(Optional.of(subjectB_Math));
        when(subjectRepository.findByIdAndSchoolId(subjectIdB_Math, schoolIdA))
                .thenReturn(Optional.empty());
        when(subjectRepository.findByIdAndSchoolId(subjectIdB_Eng, schoolIdB))
                .thenReturn(Optional.of(subjectB_Eng));
        when(subjectRepository.findByIdAndSchoolId(subjectIdB_Eng, schoolIdA))
                .thenReturn(Optional.empty());

        when(standardSubjectRepository.findBySchoolIdAndStandardIdOrderBySortOrder(
                        schoolIdB, standardIdB))
                .thenReturn(List.of(mappingB_Math, mappingB_Eng));
        when(standardSubjectRepository.findByIdAndSchoolId(mappingB_Math.getId(), schoolIdB))
                .thenReturn(Optional.of(mappingB_Math));
        when(standardSubjectRepository.findByIdAndSchoolId(mappingB_Math.getId(), schoolIdA))
                .thenReturn(Optional.empty());

        when(academicYearRepository.findBySchoolIdAndStatus(schoolIdB, AcademicYearStatus.CURRENT))
                .thenReturn(Optional.of(yearB));
        when(academicYearRepository.findByIdAndSchoolId(yearIdB, schoolIdB))
                .thenReturn(Optional.of(yearB));
        when(academicYearRepository.findByIdAndSchoolId(yearIdB, schoolIdA))
                .thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("Tenant A reads standards: sees only School A standard, classes, and signature")
    void publicStandards_withTenantA_shouldReturnOnlySchoolAStandards() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/standards")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(standardIdA.toString()))
                .andExpect(jsonPath("$[0].code").value("STD_10"))
                .andExpect(jsonPath("$[0].displayName").value("ધોરણ ૧૦ (School A)"))
                .andExpect(jsonPath("$[0].classTeacherName").value("પટેલ રમેશભાઈ"))
                .andExpect(
                        jsonPath("$[0].classTeacherSignatureUrl")
                                .value(
                                        "https://ik.imagekit.io/signatures/"
                                                + schoolIdA
                                                + "/class-teacher.png"))
                .andExpect(jsonPath("$[0].classes[0]").value("૧૦-અ"))
                .andExpect(jsonPath("$[0].classes[1]").value("૧૦-બ"))
                // Ensure School B details are not leaked
                .andExpect(
                        jsonPath("$[0].displayName")
                                .value(org.hamcrest.Matchers.not("ધોરણ ૧૦ (School B)")))
                .andExpect(
                        jsonPath("$[0].classTeacherName")
                                .value(org.hamcrest.Matchers.not("શાહ સુરેશભાઈ")));
    }

    @Test
    @DisplayName("Tenant B reads standards: sees only School B standard and classes")
    void publicStandards_withTenantB_shouldReturnOnlySchoolBStandards() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/standards")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(standardIdB.toString()))
                .andExpect(jsonPath("$[0].code").value("STD_10"))
                .andExpect(jsonPath("$[0].displayName").value("ધોરણ ૧૦ (School B)"))
                .andExpect(jsonPath("$[0].classTeacherName").value("શાહ સુરેશભાઈ"))
                .andExpect(jsonPath("$[0].classTeacherSignatureUrl").doesNotExist())
                .andExpect(jsonPath("$[0].classes[0]").value("૧૦-ક"))
                .andExpect(jsonPath("$[0].classes[1]").value("૧૦-ડ"))
                // Ensure School A details are not leaked
                .andExpect(
                        jsonPath("$[0].displayName")
                                .value(org.hamcrest.Matchers.not("ધોરણ ૧૦ (School A)")));
    }

    @Test
    @DisplayName("Tenant A reads subjects: sees MATH and SCI for School A, not ENG from School B")
    void publicSubjects_withTenantA_shouldReturnOnlySchoolASubjects() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/subjects")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(subjectIdA_Math.toString()))
                .andExpect(jsonPath("$[0].code").value("MATH"))
                .andExpect(jsonPath("$[0].name").value("ગણિત (School A)"))
                .andExpect(jsonPath("$[1].id").value(subjectIdA_Sci.toString()))
                .andExpect(jsonPath("$[1].code").value("SCI"))
                .andExpect(jsonPath("$[1].name").value("વિજ્ઞાન (School A)"));
    }

    @Test
    @DisplayName("Tenant B reads subjects: sees MATH and ENG for School B, not SCI from School A")
    void publicSubjects_withTenantB_shouldReturnOnlySchoolBSubjects() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/subjects")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(subjectIdB_Math.toString()))
                .andExpect(jsonPath("$[0].code").value("MATH"))
                .andExpect(jsonPath("$[0].name").value("ગણિત (School B)"))
                .andExpect(jsonPath("$[1].id").value(subjectIdB_Eng.toString()))
                .andExpect(jsonPath("$[1].code").value("ENG"))
                .andExpect(jsonPath("$[1].name").value("અંગ્રેજી (School B)"));
    }

    @Test
    @DisplayName("Tenant A reads current academic year: returns School A current year")
    void publicCurrentYear_withTenantA_shouldReturnSchoolACurrentYear() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/academic-years/current")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(yearIdA.toString()))
                .andExpect(jsonPath("$.name").value("૨૦૨૬-૨૦૨૭ (A)"))
                .andExpect(jsonPath("$.startsOn").value("2026-06-01"))
                .andExpect(jsonPath("$.endsOn").value("2027-04-30"))
                .andExpect(jsonPath("$.status").value("CURRENT"));
    }

    @Test
    @DisplayName("Tenant B reads current academic year: returns School B current year")
    void publicCurrentYear_withTenantB_shouldReturnSchoolBCurrentYear() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/academic-years/current")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_B))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(yearIdB.toString()))
                .andExpect(jsonPath("$.name").value("૨૦૨૬-૨૦૨૭ (B)"))
                .andExpect(jsonPath("$.startsOn").value("2026-06-15"))
                .andExpect(jsonPath("$.endsOn").value("2027-05-15"))
                .andExpect(jsonPath("$.status").value("CURRENT"));
    }

    @Test
    @DisplayName("When school has no current year configured: returns 404 academic_year_not_found")
    void publicCurrentYear_whenNotConfigured_shouldReturnNotFound() throws Exception {
        when(academicYearRepository.findBySchoolIdAndStatus(schoolIdA, AcademicYearStatus.CURRENT))
                .thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/v1/public/academic-years/current")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_A))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("academic_year_not_found"));
    }

    @Test
    @DisplayName("Public academic endpoint with missing gateway token: 403 Forbidden")
    void publicEndpoints_withMissingGatewayToken_shouldRejectForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/public/standards"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));

        mockMvc.perform(get("/api/v1/public/subjects"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));

        mockMvc.perform(get("/api/v1/public/academic-years/current"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("missing_gateway_credential"));
    }

    @Test
    @DisplayName("Public academic endpoint with invalid gateway token: 403 Forbidden")
    void publicEndpoints_withInvalidGatewayToken_shouldRejectForbidden() throws Exception {
        when(tenantResolver.resolveTenant("unknown-token")).thenReturn(Optional.empty());

        mockMvc.perform(
                        get("/api/v1/public/standards")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        "unknown-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }

    @Test
    @DisplayName("Public academic endpoint with inactive tenant token: 403 Forbidden")
    void publicEndpoints_withInactiveTenantToken_shouldRejectForbidden() throws Exception {
        mockMvc.perform(
                        get("/api/v1/public/standards")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        TOKEN_INACTIVE))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("invalid_gateway_credential"));
    }

    @Test
    @DisplayName(
            "Public academic endpoint when token resolves to inactive school in DB: 404 school_not_found")
    void publicEndpoints_withInactiveSchoolInDatabase_shouldReturnNotFound() throws Exception {
        when(tenantResolver.resolveTenant("valid-token-for-inactive-school"))
                .thenReturn(Optional.of(new TenantContext(schoolIdInactive, "school-inactive")));

        mockMvc.perform(
                        get("/api/v1/public/standards")
                                .header(
                                        GatewayCredentialAuthenticationFilter.GATEWAY_TOKEN_HEADER,
                                        "valid-token-for-inactive-school"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("school_not_found"));
    }

    @Test
    @DisplayName(
            "Negative test: Tenant A cannot read School B subject mappings using guessed standard UUID")
    void cannotReadAnotherSchoolStandardSubjectMappingsUsingGuessedStandardId() {
        // Tenant A tries to read mappings for standardIdB (which belongs to School B)
        assertThatThrownBy(() -> academicService.mappings(schoolIdA, standardIdB))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        ex -> {
                            DomainException de = (DomainException) ex;
                            assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(de.getCode()).isEqualTo("standard_not_found");
                        });
    }

    @Test
    @DisplayName(
            "Negative test: Tenant A cannot read/modify School B academic year using guessed year UUID")
    void cannotReadAnotherSchoolAcademicYearUsingGuessedYearId() {
        // Tenant A tries to mark current or archive School B's yearIdB
        assertThatThrownBy(() -> academicService.markCurrent(schoolIdA, UUID.randomUUID(), yearIdB))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        ex -> {
                            DomainException de = (DomainException) ex;
                            assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(de.getCode()).isEqualTo("academic_year_not_found");
                        });
    }

    @Test
    @DisplayName(
            "Negative test: Tenant A cannot modify School B standard using guessed standard UUID")
    void cannotModifyAnotherSchoolStandardUsingGuessedStandardId() {
        assertThatThrownBy(
                        () ->
                                academicService.deleteStandard(
                                        schoolIdA, UUID.randomUUID(), standardIdB))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        ex -> {
                            DomainException de = (DomainException) ex;
                            assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(de.getCode()).isEqualTo("standard_not_found");
                        });
    }

    @Test
    @DisplayName(
            "Negative test: Tenant A cannot modify School B subject using guessed subject UUID")
    void cannotModifyAnotherSchoolSubjectUsingGuessedSubjectId() {
        assertThatThrownBy(
                        () ->
                                academicService.deleteSubject(
                                        schoolIdA, UUID.randomUUID(), subjectIdB_Math))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        ex -> {
                            DomainException de = (DomainException) ex;
                            assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(de.getCode()).isEqualTo("subject_not_found");
                        });
    }

    @Test
    @DisplayName(
            "Negative test: Tenant A cannot update School B standard-subject mapping using guessed mapping UUID")
    void cannotModifyAnotherSchoolStandardSubjectMappingUsingGuessedMappingId() {
        StandardSubjectRequest request =
                new StandardSubjectRequest(standardIdB, subjectIdB_Math, (short) 1, 90);

        assertThatThrownBy(
                        () ->
                                academicService.updateMapping(
                                        schoolIdA,
                                        UUID.randomUUID(),
                                        mappingB_Math.getId(),
                                        request))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        ex -> {
                            DomainException de = (DomainException) ex;
                            assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(de.getCode()).isEqualTo("standard_subject_not_found");
                        });
    }

    @Test
    @DisplayName(
            "Academic Setup isolation: same standard/subject codes in two schools yield distinct mappings")
    void academicSetup_shouldIsolateStandardsAndSubjectMappingsBetweenSchoolsWithSameCodes() {
        var setupA = academicService.academicSetup(schoolIdA);
        assertThat(setupA.standards()).hasSize(1);
        var standardItemA = setupA.standards().getFirst();
        assertThat(standardItemA.standard().code()).isEqualTo("STD_10");
        assertThat(standardItemA.standard().displayName()).isEqualTo("ધોરણ ૧૦ (School A)");
        assertThat(standardItemA.subjects()).hasSize(2);
        assertThat(standardItemA.subjects().stream().map(SubjectResponse::code).toList())
                .containsExactly("MATH", "SCI");

        var setupB = academicService.academicSetup(schoolIdB);
        assertThat(setupB.standards()).hasSize(1);
        var standardItemB = setupB.standards().getFirst();
        assertThat(standardItemB.standard().code()).isEqualTo("STD_10");
        assertThat(standardItemB.standard().displayName()).isEqualTo("ધોરણ ૧૦ (School B)");
        assertThat(standardItemB.subjects()).hasSize(2);
        assertThat(standardItemB.subjects().stream().map(SubjectResponse::code).toList())
                .containsExactly("MATH", "ENG");
    }
}
