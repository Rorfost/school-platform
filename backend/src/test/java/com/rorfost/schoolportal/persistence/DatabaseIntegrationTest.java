package com.rorfost.schoolportal.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rorfost.schoolportal.academic.domain.AcademicYear;
import com.rorfost.schoolportal.academic.domain.AcademicYearStatus;
import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.StandardSubject;
import com.rorfost.schoolportal.academic.domain.Student;
import com.rorfost.schoolportal.academic.domain.Subject;
import com.rorfost.schoolportal.academic.repository.AcademicYearRepository;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.StudentRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.assessment.domain.Assessment;
import com.rorfost.schoolportal.assessment.domain.AssessmentSubject;
import com.rorfost.schoolportal.assessment.domain.Mark;
import com.rorfost.schoolportal.assessment.repository.AssessmentRepository;
import com.rorfost.schoolportal.assessment.repository.AssessmentSubjectRepository;
import com.rorfost.schoolportal.assessment.repository.MarkRepository;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class DatabaseIntegrationTest {

  private static final String BCRYPT_HASH =
      "$2a$10$7EqJtq98hPqEX7fNZaFWoO5uDkG1aN4eFSXYO1VK1uN4xM3MgiVFO";
  private static final UUID UNIT_TEST_TYPE_ID =
      UUID.fromString("00000000-0000-0000-0000-000000000002");

  @Container
  static final PostgreSQLContainer<?> postgres =
      new PostgreSQLContainer<>("postgres:16-alpine")
          .withDatabaseName("school_portal_test")
          .withUsername("school_portal")
          .withPassword("school_portal_test");

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
  }

  @Autowired private SchoolRepository schoolRepository;
  @Autowired private AcademicYearRepository academicYearRepository;
  @Autowired private StandardRepository standardRepository;
  @Autowired private SubjectRepository subjectRepository;
  @Autowired private StandardSubjectRepository standardSubjectRepository;
  @Autowired private StudentRepository studentRepository;
  @Autowired private AssessmentRepository assessmentRepository;
  @Autowired private AssessmentSubjectRepository assessmentSubjectRepository;
  @Autowired private MarkRepository markRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @Test
  void rejectsDuplicateStudentRollWithinTheSameAcademicScope() {
    AcademicScope scope = createScope();
    studentRepository.saveAndFlush(student(scope, "7"));

    assertThatThrownBy(() -> studentRepository.saveAndFlush(student(scope, "7")))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void permitsTheSameRollInAnotherStandard() {
    AcademicScope scope = createScope();
    studentRepository.saveAndFlush(student(scope, "7"));
    Standard anotherStandard =
        standardRepository.saveAndFlush(
            new Standard(scope.schoolId(), "2", "Standard 2", (short) 2));

    studentRepository.saveAndFlush(
        new Student(
            scope.schoolId(),
            scope.academicYearId(),
            anotherStandard.getId(),
            "Another Student",
            "7",
            BCRYPT_HASH));

    assertThat(
            studentRepository.findBySchoolIdAndAcademicYearIdAndStandardIdAndRollNumber(
                scope.schoolId(), scope.academicYearId(), anotherStandard.getId(), "7"))
        .isPresent();
  }

  @Test
  void rejectsDuplicateStandardSubjectMappings() {
    AcademicScope scope = createScope();

    assertThatThrownBy(
            () ->
                standardSubjectRepository.saveAndFlush(
                    new StandardSubject(scope.schoolId(), scope.standardId(), scope.subjectId())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsDuplicateStandardNamesAndDisplayOrderWithinASchool() {
    AcademicScope scope = createScope();

    assertThatThrownBy(
            () ->
                standardRepository.saveAndFlush(
                    new Standard(scope.schoolId(), "ONE_AGAIN", "standard 1", (short) 2)))
        .isInstanceOf(DataIntegrityViolationException.class);
    assertThatThrownBy(
            () ->
                standardRepository.saveAndFlush(
                    new Standard(scope.schoolId(), "TWO", "Standard 2", (short) 1)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsDuplicateMarksForTheSameStudentAssessmentAndSubject() {
    AcademicScope scope = createScope();
    Student student = studentRepository.saveAndFlush(student(scope, "7"));
    Assessment assessment =
        assessmentRepository.saveAndFlush(
            new Assessment(
                scope.schoolId(),
                scope.academicYearId(),
                scope.standardId(),
                UNIT_TEST_TYPE_ID,
                "Unit Test 1"));
    AssessmentSubject assessmentSubject =
        assessmentSubjectRepository.saveAndFlush(
            new AssessmentSubject(
                scope.schoolId(),
                assessment.getId(),
                scope.standardId(),
                scope.standardSubjectId()));

    markRepository.saveAndFlush(
        new Mark(
            scope.schoolId(),
            scope.academicYearId(),
            scope.standardId(),
            assessment.getId(),
            student.getId(),
            assessmentSubject.getId(),
            new BigDecimal("42.50")));

    assertThatThrownBy(
            () ->
                markRepository.saveAndFlush(
                    new Mark(
                        scope.schoolId(),
                        scope.academicYearId(),
                        scope.standardId(),
                        assessment.getId(),
                        student.getId(),
                        assessmentSubject.getId(),
                        new BigDecimal("43.00"))))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void rejectsAStudentThatReferencesAnUnknownAcademicScope() {
    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    """
                    INSERT INTO students (
                        id, school_id, academic_year_id, standard_id, full_name, roll_number, result_pin_hash
                    ) VALUES (?, ?, ?, ?, ?, ?, ?)
                    """,
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "Unknown",
                    "1",
                    BCRYPT_HASH))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void permitsOnlyOneCurrentAcademicYearPerSchool() {
    AcademicScope scope = createScope();

    assertThatThrownBy(
            () ->
                academicYearRepository.saveAndFlush(
                    new AcademicYear(
                        scope.schoolId(),
                        "2027-28",
                        LocalDate.of(2027, 6, 1),
                        LocalDate.of(2028, 5, 31),
                        AcademicYearStatus.CURRENT,
                        null)))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void restrictsAcademicConfigurationDeletionAndCascadesAlbumImages() {
    AcademicScope scope = createScope();

    assertThatThrownBy(
            () -> jdbcTemplate.update("DELETE FROM standards WHERE id = ?", scope.standardId()))
        .isInstanceOf(DataIntegrityViolationException.class);

    UUID albumId = UUID.randomUUID();
    UUID imageId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO gallery_albums (id, school_id, title) VALUES (?, ?, ?)",
        albumId,
        scope.schoolId(),
        "School day");
    jdbcTemplate.update(
        """
        INSERT INTO gallery_images (
            id, school_id, gallery_album_id, storage_bucket, object_key, original_filename,
            content_type, byte_size, alt_text, sort_order
        ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        imageId,
        scope.schoolId(),
        albumId,
        "test-private",
        "gallery/test.jpg",
        "test.jpg",
        "image/jpeg",
        1,
        "School day photo",
        1);

    jdbcTemplate.update("DELETE FROM gallery_albums WHERE id = ?", albumId);

    assertThat(
            jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gallery_images WHERE id = ?", Integer.class, imageId))
        .isZero();
  }

  @Test
  void rejectsAnInvalidPublicationStateAndUpdatesTimestampsInTheDatabase() {
    AcademicScope scope = createScope();
    UUID albumId = UUID.randomUUID();
    jdbcTemplate.update(
        "INSERT INTO gallery_albums (id, school_id, title) VALUES (?, ?, ?)",
        albumId,
        scope.schoolId(),
        "Annual day");

    assertThatThrownBy(
            () ->
                jdbcTemplate.update(
                    "UPDATE gallery_albums SET published_at = CURRENT_TIMESTAMP WHERE id = ?",
                    albumId))
        .isInstanceOf(DataIntegrityViolationException.class);

    School school = schoolRepository.saveAndFlush(new School("Timestamp School", uniqueSlug()));
    Instant createdAt =
        jdbcTemplate.queryForObject(
            "SELECT created_at FROM schools WHERE id = ?", Instant.class, school.getId());
    jdbcTemplate.execute("SELECT pg_sleep(0.01)");
    jdbcTemplate.update(
        "UPDATE schools SET name = ? WHERE id = ?", "Renamed School", school.getId());
    Instant updatedAt =
        jdbcTemplate.queryForObject(
            "SELECT updated_at FROM schools WHERE id = ?", Instant.class, school.getId());

    assertThat(updatedAt).isAfter(createdAt);
  }

  private AcademicScope createScope() {
    School school = schoolRepository.saveAndFlush(new School("Test School", uniqueSlug()));
    AcademicYear academicYear =
        academicYearRepository.saveAndFlush(
            new AcademicYear(
                school.getId(),
                "2026-27",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2027, 5, 31),
                AcademicYearStatus.CURRENT,
                null));
    Standard standard =
        standardRepository.saveAndFlush(new Standard(school.getId(), "1", "Standard 1", (short) 1));
    Subject subject =
        subjectRepository.saveAndFlush(
            new Subject(school.getId(), "MATH", "Mathematics", (short) 1));
    StandardSubject standardSubject =
        standardSubjectRepository.saveAndFlush(
            new StandardSubject(school.getId(), standard.getId(), subject.getId()));
    return new AcademicScope(
        school.getId(),
        academicYear.getId(),
        standard.getId(),
        subject.getId(),
        standardSubject.getId());
  }

  private Student student(AcademicScope scope, String rollNumber) {
    return new Student(
        scope.schoolId(),
        scope.academicYearId(),
        scope.standardId(),
        "Student",
        rollNumber,
        BCRYPT_HASH);
  }

  private String uniqueSlug() {
    return "test-" + UUID.randomUUID();
  }

  private record AcademicScope(
      UUID schoolId,
      UUID academicYearId,
      UUID standardId,
      UUID subjectId,
      UUID standardSubjectId) {}
}
