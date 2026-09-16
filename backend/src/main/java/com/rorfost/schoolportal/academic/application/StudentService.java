package com.rorfost.schoolportal.academic.application;

import com.rorfost.schoolportal.academic.api.StudentRequest;
import com.rorfost.schoolportal.academic.api.StudentResponse;
import com.rorfost.schoolportal.academic.domain.Student;
import com.rorfost.schoolportal.academic.repository.StudentRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class StudentService {
  private final StudentRepository studentRepository;
  private final PasswordEncoder passwordEncoder;

  public StudentService(StudentRepository studentRepository, PasswordEncoder passwordEncoder) {
    this.studentRepository = studentRepository;
    this.passwordEncoder = passwordEncoder;
  }

  public List<StudentResponse> listByStandard(UUID schoolId, UUID standardId) {
    return studentRepository.findBySchoolIdAndStandardId(schoolId, standardId).stream()
        .map(StudentResponse::from)
        .toList();
  }

  @Transactional
  public StudentResponse createStudent(UUID schoolId, StudentRequest request) {
    studentRepository
        .findBySchoolIdAndAcademicYearIdAndStandardIdAndRollNumber(
            schoolId, request.academicYearId(), request.standardId(), request.rollNumber())
        .ifPresent(
            s -> {
              throw new DomainException(HttpStatus.CONFLICT, "student_roll_number_exists");
            });

    String pin =
        (request.resultPin() != null && !request.resultPin().isBlank())
            ? request.resultPin()
            : "123456";
    String pinHash = passwordEncoder.encode(pin);

    Student student =
        new Student(
            schoolId,
            request.academicYearId(),
            request.standardId(),
            request.fullName(),
            request.rollNumber(),
            pinHash);

    return StudentResponse.from(studentRepository.save(student));
  }

  @Transactional
  public StudentResponse updateStudent(UUID schoolId, UUID id, StudentRequest request) {
    Student student =
        studentRepository
            .findById(id)
            .filter(s -> s.getSchoolId().equals(schoolId))
            .orElseThrow(() -> new DomainException(HttpStatus.NOT_FOUND, "student_not_found"));

    String pinHash =
        (request.resultPin() != null && !request.resultPin().isBlank())
            ? passwordEncoder.encode(request.resultPin())
            : null;

    student.update(request.fullName(), request.rollNumber(), pinHash, request.isArchived());
    return StudentResponse.from(studentRepository.save(student));
  }
}
