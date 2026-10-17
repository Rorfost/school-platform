package com.rorfost.schoolportal.school.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.api.VisitResponse;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Optional;
import java.util.UUID;

class VisitServiceTest {

    private JdbcTemplate jdbc;
    private SchoolRepository schools;
    private VisitService service;

    private UUID schoolId;
    private School school;

    @BeforeEach
    void setUp() {
        jdbc = mock(JdbcTemplate.class);
        schools = mock(SchoolRepository.class);
        service = new VisitService(jdbc, schools);

        schoolId = UUID.randomUUID();
        school = mock(School.class);
        when(school.getId()).thenReturn(schoolId);
        when(schools.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));
    }

    @Test
    void increment_shouldExecuteUpsertAndReturnCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(schoolId))).thenReturn(1L);

        VisitResponse response = service.increment(schoolId);

        assertThat(response.totalVisits()).isEqualTo(1L);
        verify(jdbc).update(anyString(), eq(schoolId));
    }

    @Test
    void get_shouldReturnCurrentCount() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(schoolId))).thenReturn(42L);

        VisitResponse response = service.get(schoolId);

        assertThat(response.totalVisits()).isEqualTo(42L);
    }

    @Test
    void get_shouldReturnZeroWhenNullFromDb() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(schoolId))).thenReturn(null);

        VisitResponse response = service.get(schoolId);

        assertThat(response.totalVisits()).isEqualTo(0L);
    }

    @Test
    void reset_shouldResetCountToZero() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq(schoolId))).thenReturn(0L);

        VisitResponse response = service.reset(schoolId);

        assertThat(response.totalVisits()).isEqualTo(0L);
        verify(jdbc).update(anyString(), eq(schoolId));
    }

    @Test
    void shouldThrowWhenSchoolInactiveOrNotFound() {
        UUID unknownSchoolId = UUID.randomUUID();
        when(schools.findByIdAndIsActiveTrue(unknownSchoolId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.increment(unknownSchoolId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "school_not_found")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        assertThatThrownBy(() -> service.get(unknownSchoolId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "school_not_found")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);

        assertThatThrownBy(() -> service.reset(unknownSchoolId))
                .isInstanceOf(DomainException.class)
                .hasFieldOrPropertyWithValue("code", "school_not_found")
                .hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }
}
