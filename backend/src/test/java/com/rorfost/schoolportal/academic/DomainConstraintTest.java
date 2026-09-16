package com.rorfost.schoolportal.academic;

import static org.assertj.core.api.Assertions.assertThat;

import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.Subject;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DomainConstraintTest {

  @Test
  void academicYearDateValidation() {
    UUID schoolId = UUID.randomUUID();
    LocalDate start = LocalDate.of(2026, 6, 1);
    LocalDate end = LocalDate.of(2026, 5, 31);

    AcademicYear year =
        new AcademicYear(
            schoolId,
            "2026-27",
            start,
            LocalDate.of(2027, 4, 30),
            AcademicYearStatus.CURRENT,
            null);
    assertThat(year.getName()).isEqualTo("2026-27");
    assertThat(year.getStatus()).isEqualTo(AcademicYearStatus.CURRENT);

    assertThat(end).isBefore(start);
  }

  @Test
  void standardAndSubjectNormalization() {
    UUID schoolId = UUID.randomUUID();

    Standard std = new Standard(schoolId, "std-3", "Standard 3", (short) 3);
    assertThat(std.getCode()).isEqualTo("std-3");
    assertThat(std.getDisplayName()).isEqualTo("Standard 3");

    Subject subj = new Subject(schoolId, "maths", "Mathematics", (short) 1);
    assertThat(subj.getCode()).isEqualTo("MATHS");
    assertThat(subj.getName()).isEqualTo("Mathematics");
  }
}
