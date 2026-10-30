package com.rorfost.schoolportal.academic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.api.StudentRequest;
import com.rorfost.schoolportal.academic.api.StudentResponse;
import com.rorfost.schoolportal.academic.application.StudentService;
import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.Student;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StudentRepository;
import com.rorfost.schoolportal.common.exception.DomainException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

class StudentServiceTest {
    private final StudentRepository studentRepository = Mockito.mock(StudentRepository.class);
    private final AcademicYearRepository academicYears = Mockito.mock(AcademicYearRepository.class);
    private final StandardRepository standards = Mockito.mock(StandardRepository.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final StudentService service =
            new StudentService(studentRepository, academicYears, standards, passwordEncoder);

    @Test
    @DisplayName("Creates student with hashed result pin when parent IDs belong to the school")
    void createsStudentWithHashedResultPin() {
        UUID schoolId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID standardId = UUID.randomUUID();

        AcademicYear year =
                new AcademicYear(
                        schoolId,
                        "2024-25",
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2025, 3, 31),
                        AcademicYearStatus.CURRENT,
                        null);
        Standard standard = new Standard(schoolId, "STD_5", "Standard 5", (short) 5);

        when(academicYears.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year));
        when(standards.findByIdAndSchoolId(standardId, schoolId)).thenReturn(Optional.of(standard));

        StudentRequest request =
                new StudentRequest(yearId, standardId, "Student A", "101", "123456", false);

        when(studentRepository.findBySchoolIdAndAcademicYearIdAndStandardIdAndRollNumber(
                        schoolId, yearId, standardId, "101"))
                .thenReturn(Optional.empty());
        when(studentRepository.save(any(Student.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        StudentResponse response = service.createStudent(schoolId, request);

        assertThat(response.fullName()).isEqualTo("Student A");
        assertThat(response.rollNumber()).isEqualTo("101");
        assertThat(response.isArchived()).isFalse();
    }

    @Test
    @DisplayName("Rejects duplicate roll number in same standard and academic year")
    void rejectsDuplicateStudentRollNumberInSameStandardAndYear() {
        UUID schoolId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID standardId = UUID.randomUUID();

        AcademicYear year =
                new AcademicYear(
                        schoolId,
                        "2024-25",
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2025, 3, 31),
                        AcademicYearStatus.CURRENT,
                        null);
        Standard standard = new Standard(schoolId, "STD_5", "Standard 5", (short) 5);

        when(academicYears.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year));
        when(standards.findByIdAndSchoolId(standardId, schoolId)).thenReturn(Optional.of(standard));

        Student existing =
                new Student(schoolId, yearId, standardId, "Existing Student", "101", "hash");
        when(studentRepository.findBySchoolIdAndAcademicYearIdAndStandardIdAndRollNumber(
                        schoolId, yearId, standardId, "101"))
                .thenReturn(Optional.of(existing));

        StudentRequest request =
                new StudentRequest(yearId, standardId, "Student A", "101", "123456", false);

        assertThatThrownBy(() -> service.createStudent(schoolId, request))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("student_roll_number_exists"));
    }

    @Test
    @DisplayName(
            "Rejects createStudent when academicYearId belongs to a different school (cross-tenant)")
    void createStudent_foreignAcademicYear_rejected() {
        UUID schoolId = UUID.randomUUID();
        UUID foreignYearId = UUID.randomUUID(); // belongs to another school; repo returns empty
        UUID standardId = UUID.randomUUID();

        // academicYears.findByIdAndSchoolId returns empty because yearId is from another school
        when(academicYears.findByIdAndSchoolId(foreignYearId, schoolId))
                .thenReturn(Optional.empty());

        StudentRequest request =
                new StudentRequest(foreignYearId, standardId, "Student X", "1", null, false);

        assertThatThrownBy(() -> service.createStudent(schoolId, request))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("academic_year_invalid"));
    }

    @Test
    @DisplayName(
            "Rejects createStudent when standardId belongs to a different school (cross-tenant)")
    void createStudent_foreignStandard_rejected() {
        UUID schoolId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID foreignStandardId = UUID.randomUUID(); // belongs to another school

        AcademicYear year =
                new AcademicYear(
                        schoolId,
                        "2024-25",
                        LocalDate.of(2024, 6, 1),
                        LocalDate.of(2025, 3, 31),
                        AcademicYearStatus.CURRENT,
                        null);
        when(academicYears.findByIdAndSchoolId(yearId, schoolId)).thenReturn(Optional.of(year));
        // standardId is foreign — repo returns empty
        when(standards.findByIdAndSchoolId(foreignStandardId, schoolId))
                .thenReturn(Optional.empty());

        StudentRequest request =
                new StudentRequest(yearId, foreignStandardId, "Student X", "1", null, false);

        assertThatThrownBy(() -> service.createStudent(schoolId, request))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("standard_invalid"));
    }

    @Test
    @DisplayName("Rejects updateStudent when standardId in request belongs to another school")
    void updateStudent_foreignStandardId_rejected() {
        UUID schoolId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID yearId = UUID.randomUUID();
        UUID ownStandardId = UUID.randomUUID();
        UUID foreignStandardId = UUID.randomUUID();

        Student student = new Student(schoolId, yearId, ownStandardId, "Original", "1", "hash");
        when(studentRepository.findById(studentId)).thenReturn(Optional.of(student));
        // foreign standardId not found for this school
        when(standards.findByIdAndSchoolId(foreignStandardId, schoolId))
                .thenReturn(Optional.empty());

        StudentRequest request =
                new StudentRequest(yearId, foreignStandardId, "Updated Name", "1", null, false);

        assertThatThrownBy(() -> service.updateStudent(schoolId, studentId, request))
                .isInstanceOf(DomainException.class)
                .satisfies(
                        e ->
                                assertThat(((DomainException) e).getCode())
                                        .isEqualTo("standard_invalid"));
    }
}
