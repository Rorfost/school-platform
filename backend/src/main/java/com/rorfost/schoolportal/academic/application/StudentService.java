package com.rorfost.schoolportal.academic.application;

import com.rorfost.schoolportal.academic.api.StudentRequest;
import com.rorfost.schoolportal.academic.api.StudentResponse;
import com.rorfost.schoolportal.academic.domain.Student;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StudentRepository;
import com.rorfost.schoolportal.common.exception.DomainException;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentService {
    private final StudentRepository studentRepository;
    private final AcademicYearRepository academicYears;
    private final StandardRepository standards;
    private final PasswordEncoder passwordEncoder;

    public StudentService(
            StudentRepository studentRepository,
            AcademicYearRepository academicYears,
            StandardRepository standards,
            PasswordEncoder passwordEncoder) {
        this.studentRepository = studentRepository;
        this.academicYears = academicYears;
        this.standards = standards;
        this.passwordEncoder = passwordEncoder;
    }

    public List<StudentResponse> listByStandard(UUID schoolId, UUID standardId) {
        // Verify the standard belongs to this school before listing its students.
        requireStandard(schoolId, standardId);
        return studentRepository.findBySchoolIdAndStandardId(schoolId, standardId).stream()
                .map(StudentResponse::from)
                .toList();
    }

    @Transactional
    public StudentResponse createStudent(UUID schoolId, StudentRequest request) {
        // Validate parent IDs belong to this school; never trust a client-supplied schoolId.
        requireAcademicYear(schoolId, request.academicYearId());
        requireStandard(schoolId, request.standardId());

        studentRepository
                .findBySchoolIdAndAcademicYearIdAndStandardIdAndRollNumber(
                        schoolId,
                        request.academicYearId(),
                        request.standardId(),
                        request.rollNumber())
                .ifPresent(
                        s -> {
                            throw new DomainException(
                                    HttpStatus.CONFLICT, "student_roll_number_exists");
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
                        .orElseThrow(
                                () ->
                                        new DomainException(
                                                HttpStatus.NOT_FOUND, "student_not_found"));

        // Re-validate standardId on update so the standard cannot be re-pointed to another
        // school's record even when the student entity itself was located by school.
        requireStandard(schoolId, request.standardId());

        String pinHash =
                (request.resultPin() != null && !request.resultPin().isBlank())
                        ? passwordEncoder.encode(request.resultPin())
                        : null;

        student.update(request.fullName(), request.rollNumber(), pinHash, request.isArchived());
        return StudentResponse.from(studentRepository.save(student));
    }

    private void requireAcademicYear(UUID schoolId, UUID academicYearId) {
        academicYears
                .findByIdAndSchoolId(academicYearId, schoolId)
                .orElseThrow(
                        () -> new DomainException(HttpStatus.BAD_REQUEST, "academic_year_invalid"));
    }

    private void requireStandard(UUID schoolId, UUID standardId) {
        standards
                .findByIdAndSchoolId(standardId, schoolId)
                .orElseThrow(() -> new DomainException(HttpStatus.BAD_REQUEST, "standard_invalid"));
    }
}
