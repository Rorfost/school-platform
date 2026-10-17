package com.rorfost.schoolportal.academic.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.application.AcademicConfigurationService;
import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import com.rorfost.schoolportal.tenant.application.TenantContextHolder;
import com.rorfost.schoolportal.tenant.domain.TenantContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class PublicAcademicControllerTest {

    private AcademicConfigurationService service;
    private SchoolRepository schoolRepository;
    private AcademicYearRepository yearRepository;
    private PublicAcademicController controller;

    private UUID schoolId;
    private School school;

    @BeforeEach
    void setUp() {
        service = Mockito.mock(AcademicConfigurationService.class);
        schoolRepository = Mockito.mock(SchoolRepository.class);
        yearRepository = Mockito.mock(AcademicYearRepository.class);
        controller = new PublicAcademicController(service, schoolRepository, yearRepository);

        schoolId = UUID.randomUUID();
        school = Mockito.mock(School.class);
        when(school.getId()).thenReturn(schoolId);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void bindTenant(UUID id, String slug) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        TenantContextHolder.setTenantContext(request, new TenantContext(id, slug));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void standards_shouldReturnStandardsForVerifiedTenant() {
        bindTenant(schoolId, "school-a");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

        Standard standard = new Standard(schoolId, "STD_10", "Standard 10", (short) 1);
        standard.updateClassTeacher("Teacher Name");
        StandardResponse stdResponse = StandardResponse.from(standard, null, List.of("A", "B"));
        when(service.standards(schoolId)).thenReturn(List.of(stdResponse));

        List<StandardResponse> result = controller.standards();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("STD_10");
        assertThat(result.getFirst().displayName()).isEqualTo("Standard 10");
        assertThat(result.getFirst().classes()).containsExactly("A", "B");
        verify(service).standards(schoolId);
    }

    @Test
    void subjects_shouldReturnSubjectsForVerifiedTenant() {
        bindTenant(schoolId, "school-a");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

        SubjectResponse subjResponse =
                new SubjectResponse(UUID.randomUUID(), "MATH", "Mathematics", (short) 1, false);
        when(service.subjects(schoolId)).thenReturn(List.of(subjResponse));

        List<SubjectResponse> result = controller.subjects();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().code()).isEqualTo("MATH");
        assertThat(result.getFirst().name()).isEqualTo("Mathematics");
        verify(service).subjects(schoolId);
    }

    @Test
    void currentYear_shouldReturnCurrentYearForVerifiedTenant() {
        bindTenant(schoolId, "school-a");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

        AcademicYear currentYear =
                new AcademicYear(
                        schoolId,
                        "2026-2027",
                        LocalDate.of(2026, 6, 1),
                        LocalDate.of(2027, 4, 30),
                        AcademicYearStatus.CURRENT,
                        null);
        when(yearRepository.findBySchoolIdAndStatus(schoolId, AcademicYearStatus.CURRENT))
                .thenReturn(Optional.of(currentYear));

        AcademicYearResponse result = controller.currentYear();

        assertThat(result.name()).isEqualTo("2026-2027");
        assertThat(result.status()).isEqualTo(AcademicYearStatus.CURRENT);
        verify(yearRepository).findBySchoolIdAndStatus(schoolId, AcademicYearStatus.CURRENT);
    }

    @Test
    void currentYear_shouldThrowWhenNoCurrentYearFound() {
        bindTenant(schoolId, "school-a");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));
        when(yearRepository.findBySchoolIdAndStatus(schoolId, AcademicYearStatus.CURRENT))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.currentYear())
                .isInstanceOf(DomainException.class)
                .satisfies(
                        ex -> {
                            DomainException de = (DomainException) ex;
                            assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(de.getCode()).isEqualTo("academic_year_not_found");
                        });
    }

    @Test
    void standards_shouldThrowNotFoundWhenTenantMissing() {
        // No tenant bound in RequestContext
        assertThatThrownBy(() -> controller.standards())
                .isInstanceOf(DomainException.class)
                .satisfies(
                        ex -> {
                            DomainException de = (DomainException) ex;
                            assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(de.getCode()).isEqualTo("school_not_found");
                        });
    }

    @Test
    void standards_shouldThrowNotFoundWhenTenantSchoolInactive() {
        bindTenant(schoolId, "school-a");
        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.standards())
                .isInstanceOf(DomainException.class)
                .satisfies(
                        ex -> {
                            DomainException de = (DomainException) ex;
                            assertThat(de.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                            assertThat(de.getCode()).isEqualTo("school_not_found");
                        });
    }
}
