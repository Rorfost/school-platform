package com.rorfost.schoolportal.academic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.rorfost.schoolportal.academic.api.StudentRequest;
import com.rorfost.schoolportal.academic.api.StudentResponse;
import com.rorfost.schoolportal.academic.application.StudentService;
import com.rorfost.schoolportal.academic.domain.Student;
import com.rorfost.schoolportal.academic.repository.StudentRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class StudentServiceTest {
  private final StudentRepository studentRepository = Mockito.mock(StudentRepository.class);
  private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

  @Test
  void createsStudentWithHashedResultPin() {
    StudentService service = new StudentService(studentRepository, passwordEncoder);
    UUID schoolId = UUID.randomUUID();
    UUID yearId = UUID.randomUUID();
    UUID standardId = UUID.randomUUID();

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
  void rejectsDuplicateStudentRollNumberInSameStandardAndYear() {
    StudentService service = new StudentService(studentRepository, passwordEncoder);
    UUID schoolId = UUID.randomUUID();
    UUID yearId = UUID.randomUUID();
    UUID standardId = UUID.randomUUID();

    Student existing = new Student(schoolId, yearId, standardId, "Existing Student", "101", "hash");
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
}
