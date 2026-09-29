package com.rorfost.schoolportal.assessment.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rorfost.schoolportal.academic.domain.Standard;
import com.rorfost.schoolportal.academic.domain.StandardSubject;
import com.rorfost.schoolportal.academic.domain.Subject;
import com.rorfost.schoolportal.academic.repository.StandardRepository;
import com.rorfost.schoolportal.academic.repository.StandardSubjectRepository;
import com.rorfost.schoolportal.academic.repository.SubjectRepository;
import com.rorfost.schoolportal.assessment.repository.ResultPresentationSettingsRepository;
import com.rorfost.schoolportal.common.exception.DomainException;
import com.rorfost.schoolportal.school.domain.AnnualExamResult;
import com.rorfost.schoolportal.school.domain.AnnualExamResultRepository;
import com.rorfost.schoolportal.school.domain.AnnualExamResultSubject;
import com.rorfost.schoolportal.school.domain.School;
import com.rorfost.schoolportal.school.repository.SchoolRepository;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class ExamResultServiceTest {

  private AnnualExamResultRepository resultRepository;
  private SchoolRepository schoolRepository;
  private ResultPresentationSettingsRepository presentationSettings;
  private StandardRepository standardRepository;
  private StandardSubjectRepository standardSubjectRepository;
  private SubjectRepository subjectRepository;

  private ExamResultService service;

  private UUID schoolId;
  private UUID standardId;

  @BeforeEach
  void setUp() {
    resultRepository = mock(AnnualExamResultRepository.class);
    schoolRepository = mock(SchoolRepository.class);
    presentationSettings = mock(ResultPresentationSettingsRepository.class);
    standardRepository = mock(StandardRepository.class);
    standardSubjectRepository = mock(StandardSubjectRepository.class);
    subjectRepository = mock(SubjectRepository.class);

    service =
        new ExamResultService(
            resultRepository,
            schoolRepository,
            presentationSettings,
            standardRepository,
            standardSubjectRepository,
            subjectRepository);

    schoolId = UUID.randomUUID();
    standardId = UUID.randomUUID();

    mockAcademicSetup();
  }

  @Test
  void trimasikUpload_shouldReadMaximumAndObtainedMarksFromExcel() throws Exception {
    MockMultipartFile file =
        createTrimasikExcelWithMax(
            "30", "20",
            "40", "18",
            "50", "22");

    service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI);

    AnnualExamResult result = getSavedResult();

    assertEquals(3, result.getSubjects().size());

    assertSubject(result.getSubjects().get(0), "Gujarati", 20, 30, "PRESENT");
    assertSubject(result.getSubjects().get(1), "Mathematics", 18, 40, "PRESENT");
    assertSubject(result.getSubjects().get(2), "English", 22, 50, "PRESENT");

    assertEquals(120, result.getTotalMarks());
    assertEquals(60, result.getObtainedMarks());
    assertEquals(50.0, result.getPercentage().doubleValue(), 0.01);
  }

  @Test
  void trimasikUpload_shouldTreatZeroAsRealMarks() throws Exception {
    MockMultipartFile file = createTrimasikExcel("0", "18", "22");

    service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI);

    AnnualExamResult result = getSavedResult();

    AnnualExamResultSubject gujarati = findSubject(result, "Gujarati");

    assertEquals(0, gujarati.getObtainedMarks());
    assertEquals("PRESENT", gujarati.getStatus());

    assertEquals(40, result.getObtainedMarks());
    assertEquals(75, result.getTotalMarks());
  }

  @Test
  void trimasikUpload_shouldTreatABAsAbsent() throws Exception {
    MockMultipartFile file = createTrimasikExcel("20", "AB", "22");

    service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI);

    AnnualExamResult result = getSavedResult();

    AnnualExamResultSubject maths = findSubject(result, "Mathematics");

    assertEquals("ABSENT", maths.getStatus());
    assertNull(maths.getObtainedMarks());
    assertNull(maths.getGrade());

    // Absent subject maximum marks must still be counted.
    assertEquals(75, result.getTotalMarks());
    assertEquals(42, result.getObtainedMarks());
  }

  @Test
  void absentSubject_shouldStillCountMaximumMarks() throws Exception {
    MockMultipartFile file = createTrimasikExcel("20", "AB", "15");

    service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI);

    AnnualExamResult result = getSavedResult();

    assertEquals(75, result.getTotalMarks());
    assertEquals(35, result.getObtainedMarks());

    assertNotNull(result.getPercentage());
    assertEquals(46.67, result.getPercentage().doubleValue(), 0.01);
  }

  @Test
  void blankObtainedMarks_shouldIgnoreSubject() throws Exception {
    MockMultipartFile file = createTrimasikExcel("20", "", "15");

    service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI);

    AnnualExamResult result = getSavedResult();

    assertEquals(2, result.getSubjects().size());

    assertNotNull(findSubject(result, "Gujarati"));
    assertNotNull(findSubject(result, "English"));

    assertTrue(
        result.getSubjects().stream()
            .noneMatch(subject -> subject.getSubjectName().equals("Mathematics")));

    // Blank obtained marks means Maths is completely ignored.
    assertEquals(50, result.getTotalMarks());
    assertEquals(35, result.getObtainedMarks());
    assertEquals(70.0, result.getPercentage().doubleValue(), 0.01);
  }

  @ParameterizedTest
  @ValueSource(strings = {"AB", "ab", "Abs", "ABS", "Absent", "ABSENT", "ગેરહાજર"})
  void differentAbsentValues_shouldBeAccepted(String absentValue) throws Exception {
    MockMultipartFile file = createTrimasikExcel("20", absentValue, "15");

    service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI);

    AnnualExamResult result = getSavedResult();

    AnnualExamResultSubject maths = findSubject(result, "Mathematics");

    assertEquals("ABSENT", maths.getStatus());
    assertNull(maths.getObtainedMarks());
    assertNull(maths.getGrade());
  }

  @Test
  void invalidObtainedMarks_shouldRejectWorkbook() throws Exception {
    MockMultipartFile file = createTrimasikExcel("20", "HELLO", "15");

    assertThrows(
        DomainException.class,
        () -> service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI));

    verify(resultRepository, never()).saveAll(any());
  }

  @Test
  void missingMaximumMarks_shouldRejectWorkbook() throws Exception {
    MockMultipartFile file =
        createTrimasikExcelWithMax(
            "25", "20",
            "", "18",
            "25", "15");

    assertThrows(
        DomainException.class,
        () -> service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI));

    verify(resultRepository, never()).saveAll(any());
  }

  @Test
  void zeroMaximumMarks_shouldRejectWorkbook() throws Exception {
    MockMultipartFile file =
        createTrimasikExcelWithMax(
            "25", "20",
            "0", "18",
            "25", "15");

    assertThrows(
        DomainException.class,
        () -> service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI));

    verify(resultRepository, never()).saveAll(any());
  }

  @Test
  void negativeMaximumMarks_shouldRejectWorkbook() throws Exception {
    MockMultipartFile file =
        createTrimasikExcelWithMax(
            "25", "20",
            "-25", "18",
            "25", "15");

    assertThrows(
        DomainException.class,
        () -> service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI));

    verify(resultRepository, never()).saveAll(any());
  }

  @Test
  void obtainedMarksGreaterThanMaximum_shouldRejectWorkbook() throws Exception {
    MockMultipartFile file =
        createTrimasikExcelWithMax(
            "25", "20",
            "25", "30",
            "25", "15");

    assertThrows(
        DomainException.class,
        () -> service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI));

    verify(resultRepository, never()).saveAll(any());
  }

  @Test
  void trimasikUpload_shouldNotSaveSubjectGrades() throws Exception {
    MockMultipartFile file = createTrimasikExcel("20", "18", "15");

    service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI);

    AnnualExamResult result = getSavedResult();

    for (AnnualExamResultSubject subject : result.getSubjects()) {
      assertNull(subject.getGrade());
    }
  }

  @Test
  void annualUpload_shouldReadMaximumObtainedAndGradeColumns() throws Exception {
    MockMultipartFile file =
        createAnnualExcel(
            "25", "20", "A",
            "50", "40", "B",
            "25", "18", "C");

    service.processExcelUpload(file, 250, ExamResultService.ANNUAL);

    AnnualExamResult result = getSavedResult();

    assertEquals(3, result.getSubjects().size());

    assertSubject(result.getSubjects().get(0), "Gujarati", 20, 25, "PRESENT");
    assertSubject(result.getSubjects().get(1), "Mathematics", 40, 50, "PRESENT");
    assertSubject(result.getSubjects().get(2), "English", 18, 25, "PRESENT");

    assertEquals("A", findSubject(result, "Gujarati").getGrade());
    assertEquals("B", findSubject(result, "Mathematics").getGrade());
    assertEquals("C", findSubject(result, "English").getGrade());

    assertEquals(100, result.getTotalMarks());
    assertEquals(78, result.getObtainedMarks());

    assertEquals(250, result.getTotalWorkingDays());
    assertEquals(233, result.getAttendedDays());
  }

  @Test
  void annualUpload_shouldIgnoreGradeWhenStudentIsAbsent() throws Exception {
    MockMultipartFile file =
        createAnnualExcel(
            "25", "20", "A",
            "25", "AB", "A",
            "25", "15", "B");

    service.processExcelUpload(file, 250, ExamResultService.ANNUAL);

    AnnualExamResult result = getSavedResult();

    AnnualExamResultSubject maths = findSubject(result, "Mathematics");

    assertEquals("ABSENT", maths.getStatus());
    assertNull(maths.getObtainedMarks());

    // Grade from Excel must not be stored for an absent subject.
    assertNull(maths.getGrade());

    assertEquals(75, result.getTotalMarks());
    assertEquals(35, result.getObtainedMarks());
  }

  @Test
  void uploadingNewTrimasikResult_shouldDeleteOldTrimasikResults() throws Exception {
    MockMultipartFile file = createTrimasikExcel("20", "18", "15");

    service.processExcelUpload(file, null, ExamResultService.EKAM_KASOTI);

    verify(resultRepository)
        .deleteBySchoolIdAndResultType(schoolId, ExamResultService.EKAM_KASOTI);

    verify(resultRepository).saveAll(any());
  }

  // -------------------------------------------------------
  // Academic setup mocks
  // -------------------------------------------------------

  private void mockAcademicSetup() {
    School school = mock(School.class);

    when(school.getId()).thenReturn(schoolId);

    when(schoolRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc())
        .thenReturn(Optional.of(school));

    when(presentationSettings.findById(schoolId)).thenReturn(Optional.empty());

    Standard standard = mock(Standard.class);

    when(standard.getId()).thenReturn(standardId);
    when(standard.getDisplayName()).thenReturn("3");
    when(standard.getCode()).thenReturn("STD_3");

    when(standardRepository.findBySchoolIdAndIsArchivedFalseOrderBySortOrder(schoolId))
        .thenReturn(List.of(standard));

    UUID gujaratiId = UUID.randomUUID();
    UUID mathsId = UUID.randomUUID();
    UUID englishId = UUID.randomUUID();

    StandardSubject gujaratiMapping = createSubjectMapping(gujaratiId);
    StandardSubject mathsMapping = createSubjectMapping(mathsId);
    StandardSubject englishMapping = createSubjectMapping(englishId);

    when(standardSubjectRepository.findBySchoolIdAndStandardIdOrderBySortOrder(
            schoolId, standardId))
        .thenReturn(List.of(gujaratiMapping, mathsMapping, englishMapping));

    mockSubject(gujaratiId, "Gujarati");
    mockSubject(mathsId, "Mathematics");
    mockSubject(englishId, "English");
  }

  private StandardSubject createSubjectMapping(UUID subjectId) {
    StandardSubject mapping = mock(StandardSubject.class);

    when(mapping.getSchoolId()).thenReturn(schoolId);
    when(mapping.getSubjectId()).thenReturn(subjectId);

    return mapping;
  }

  private void mockSubject(UUID id, String name) {
    Subject subject = mock(Subject.class);

    when(subject.getName()).thenReturn(name);

    when(subjectRepository.findByIdAndSchoolId(id, schoolId)).thenReturn(Optional.of(subject));
  }

  // -------------------------------------------------------
  // Trimasik Excel
  // -------------------------------------------------------

  private MockMultipartFile createTrimasikExcel(
      String gujaratiObtained, String mathematicsObtained, String englishObtained)
      throws Exception {

    return createTrimasikExcelWithMax(
        "25",
        gujaratiObtained,
        "25",
        mathematicsObtained,
        "25",
        englishObtained);
  }

  private MockMultipartFile createTrimasikExcelWithMax(
      String gujaratiMax,
      String gujaratiObtained,
      String mathematicsMax,
      String mathematicsObtained,
      String englishMax,
      String englishObtained)
      throws Exception {

    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Results");

      /*
       * Trimasik format:
       *
       * 0 = GR No.
       * 1 = Standard
       * 2 = Name
       * 3 = Birth Date
       *
       * Subject groups:
       * Gujarati    = Max + Obtained
       * Mathematics = Max + Obtained
       * English     = Max + Obtained
       */

      Row header = sheet.createRow(0);

      header.createCell(0).setCellValue("GR No.");
      header.createCell(1).setCellValue("Standard");
      header.createCell(2).setCellValue("Name");
      header.createCell(3).setCellValue("Birth Date");

      header.createCell(4).setCellValue("Gujarati");
      header.createCell(5).setCellValue("Obtained");

      header.createCell(6).setCellValue("Mathematics");
      header.createCell(7).setCellValue("Obtained");

      header.createCell(8).setCellValue("English");
      header.createCell(9).setCellValue("Obtained");

      Row student = sheet.createRow(1);

      student.createCell(0).setCellValue("101");
      student.createCell(1).setCellValue("3");
      student.createCell(2).setCellValue("Test Student");
      student.createCell(3).setCellValue("01/01/2018");

      student.createCell(4).setCellValue(gujaratiMax);
      student.createCell(5).setCellValue(gujaratiObtained);

      student.createCell(6).setCellValue(mathematicsMax);
      student.createCell(7).setCellValue(mathematicsObtained);

      student.createCell(8).setCellValue(englishMax);
      student.createCell(9).setCellValue(englishObtained);

      workbook.write(output);

      return new MockMultipartFile(
          "file",
          "trimasik-result.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          output.toByteArray());
    }
  }

  // -------------------------------------------------------
  // Annual Excel
  // -------------------------------------------------------

  private MockMultipartFile createAnnualExcel(
      String gujaratiMax,
      String gujaratiObtained,
      String gujaratiGrade,
      String mathematicsMax,
      String mathematicsObtained,
      String mathematicsGrade,
      String englishMax,
      String englishObtained,
      String englishGrade)
      throws Exception {

    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream output = new ByteArrayOutputStream()) {

      Sheet sheet = workbook.createSheet("Results");

      /*
       * Annual format:
       *
       * 0 = GR No.
       * 1 = Standard
       * 2 = Name
       * 3 = Birth Date
       * 4 = Hajar Divas
       *
       * Subject groups:
       * Gujarati    = Max + Obtained + Grade
       * Mathematics = Max + Obtained + Grade
       * English     = Max + Obtained + Grade
       */

      Row header = sheet.createRow(0);

      header.createCell(0).setCellValue("GR No.");
      header.createCell(1).setCellValue("Standard");
      header.createCell(2).setCellValue("Name");
      header.createCell(3).setCellValue("Birth Date");
      header.createCell(4).setCellValue("Hajar Divas");

      header.createCell(5).setCellValue("Gujarati");
      header.createCell(6).setCellValue("Obtained");
      header.createCell(7).setCellValue("Grade");

      header.createCell(8).setCellValue("Mathematics");
      header.createCell(9).setCellValue("Obtained");
      header.createCell(10).setCellValue("Grade");

      header.createCell(11).setCellValue("English");
      header.createCell(12).setCellValue("Obtained");
      header.createCell(13).setCellValue("Grade");

      Row student = sheet.createRow(1);

      student.createCell(0).setCellValue("101");
      student.createCell(1).setCellValue("3");
      student.createCell(2).setCellValue("Test Student");
      student.createCell(3).setCellValue("01/01/2018");
      student.createCell(4).setCellValue("233");

      student.createCell(5).setCellValue(gujaratiMax);
      student.createCell(6).setCellValue(gujaratiObtained);
      student.createCell(7).setCellValue(gujaratiGrade);

      student.createCell(8).setCellValue(mathematicsMax);
      student.createCell(9).setCellValue(mathematicsObtained);
      student.createCell(10).setCellValue(mathematicsGrade);

      student.createCell(11).setCellValue(englishMax);
      student.createCell(12).setCellValue(englishObtained);
      student.createCell(13).setCellValue(englishGrade);

      workbook.write(output);

      return new MockMultipartFile(
          "file",
          "annual-result.xlsx",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
          output.toByteArray());
    }
  }

  // -------------------------------------------------------
  // Assertions
  // -------------------------------------------------------

  @SuppressWarnings({"unchecked", "rawtypes"})
  private AnnualExamResult getSavedResult() {
    ArgumentCaptor<Iterable> captor = ArgumentCaptor.forClass(Iterable.class);

    verify(resultRepository).saveAll(captor.capture());

    Iterable<AnnualExamResult> iterable = captor.getValue();

    List<AnnualExamResult> results = new ArrayList<>();

    iterable.forEach(results::add);

    assertEquals(1, results.size());

    return results.get(0);
  }

  private AnnualExamResultSubject findSubject(
      AnnualExamResult result, String subjectName) {

    return result.getSubjects().stream()
        .filter(subject -> subject.getSubjectName().equals(subjectName))
        .findFirst()
        .orElseThrow();
  }

  private void assertSubject(
      AnnualExamResultSubject subject,
      String expectedName,
      Integer expectedMarks,
      int expectedMaximumMarks,
      String expectedStatus) {

    assertEquals(expectedName, subject.getSubjectName());
    assertEquals(expectedMarks, subject.getObtainedMarks());
    assertEquals(expectedMaximumMarks, subject.getMaximumMarks());
    assertEquals(expectedStatus, subject.getStatus());
  }
}
