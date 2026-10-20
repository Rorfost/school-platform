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

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

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

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

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

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

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

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

        AnnualExamResult result = getSavedResult();

        assertEquals(75, result.getTotalMarks());
        assertEquals(35, result.getObtainedMarks());

        assertNotNull(result.getPercentage());
        assertEquals(46.67, result.getPercentage().doubleValue(), 0.01);
    }

    @Test
    void blankObtainedMarks_shouldCountAsPresent_withNullMarks() throws Exception {
        // Blank obtained marks = marks not filled in = show "-" on result (not "AB").
        MockMultipartFile file = createTrimasikExcel("20", "", "15");

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

        AnnualExamResult result = getSavedResult();

        assertEquals(3, result.getSubjects().size());

        AnnualExamResultSubject maths = findSubject(result, "Mathematics");
        // Blank obtained → PRESENT with null marks (shows "-" on result, not "AB").
        assertEquals("PRESENT", maths.getStatus());
        assertNull(maths.getObtainedMarks());
        assertNull(maths.getGrade());

        // Max marks for blank subject still counted in total.
        assertEquals(75, result.getTotalMarks());
        // Blank obtained marks contribute 0 to obtained total.
        assertEquals(35, result.getObtainedMarks());
    }

    @ParameterizedTest
    @ValueSource(strings = {"AB", "ab", "Abs", "ABS", "Absent", "ABSENT", "ગેરહાજર"})
    void differentAbsentValues_shouldBeAccepted(String absentValue) throws Exception {
        MockMultipartFile file = createTrimasikExcel("20", absentValue, "15");

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

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
                () ->
                        service.processExcelUpload(
                                schoolId, file, null, ExamResultService.EKAM_KASOTI));

        verify(resultRepository, never()).saveAll(any());
    }

    @Test
    void missingMaximumMarks_shouldSkipSubject() throws Exception {
        MockMultipartFile file =
                createTrimasikExcelWithMax(
                        "25", "20",
                        "", "18",
                        "25", "15");

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

        AnnualExamResult result = getSavedResult();
        assertEquals(2, result.getSubjects().size());
        assertNotNull(findSubject(result, "Gujarati"));
        assertNotNull(findSubject(result, "English"));
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
                () ->
                        service.processExcelUpload(
                                schoolId, file, null, ExamResultService.EKAM_KASOTI));

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
                () ->
                        service.processExcelUpload(
                                schoolId, file, null, ExamResultService.EKAM_KASOTI));

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
                () ->
                        service.processExcelUpload(
                                schoolId, file, null, ExamResultService.EKAM_KASOTI));

        verify(resultRepository, never()).saveAll(any());
    }

    @Test
    void trimasikUpload_shouldNotSaveSubjectGrades() throws Exception {
        MockMultipartFile file = createTrimasikExcel("20", "18", "15");

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

        AnnualExamResult result = getSavedResult();

        for (AnnualExamResultSubject subject : result.getSubjects()) {
            assertNull(subject.getGrade());
        }
    }

    @Test
    void annualUpload_shouldReadMaximumObtainedAndGradeColumns() throws Exception {
        MockMultipartFile file =
                createAnnualExcel("25", "20", "A", "50", "40", "B", "25", "18", "C");

        service.processExcelUpload(schoolId, file, "250", ExamResultService.ANNUAL);

        AnnualExamResult result = getSavedResult();

        assertEquals(3, result.getSubjects().size());

        assertSubject(result.getSubjects().get(0), "Gujarati", 20, 25, "PRESENT");
        assertSubject(result.getSubjects().get(1), "Mathematics", 40, 50, "PRESENT");
        assertSubject(result.getSubjects().get(2), "English", 18, 25, "PRESENT");

        assertEquals("A", findSubject(result, "Gujarati").getGrade());
        assertEquals("A", findSubject(result, "Mathematics").getGrade());
        assertEquals("B", findSubject(result, "English").getGrade());

        assertEquals(100, result.getTotalMarks());
        assertEquals(78, result.getObtainedMarks());

        assertEquals("250", result.getTotalWorkingDays());
        assertEquals("233", result.getAttendedDays());
        assertEquals("A", result.getStudentClass());
    }

    @Test
    void annualUpload_shouldAcceptEmptyTotalWorkingDays() throws Exception {
        MockMultipartFile file =
                createAnnualExcel("25", "20", "A", "50", "40", "B", "25", "18", "C");

        service.processExcelUpload(schoolId, file, (String) null, ExamResultService.ANNUAL);

        AnnualExamResult result = getSavedResult();
        assertNull(result.getTotalWorkingDays());
    }

    @Test
    void annualUpload_shouldAcceptHyphenInAttendedDays() throws Exception {
        MockMultipartFile file =
                createAnnualExcelWithAttended(
                        "-", "25", "20", "A", "50", "40", "B", "25", "18", "C");

        service.processExcelUpload(schoolId, file, "250", ExamResultService.ANNUAL);

        AnnualExamResult result = getSavedResult();
        assertEquals("-", result.getAttendedDays());
    }

    @Test
    void annualUpload_shouldIgnoreGradeWhenStudentIsAbsent() throws Exception {
        MockMultipartFile file =
                createAnnualExcel("25", "20", "A", "25", "AB", "A", "25", "15", "B");

        service.processExcelUpload(schoolId, file, "250", ExamResultService.ANNUAL);

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
    void trimasikUpload_withoutPresentDivasColumn_shouldReadSubjectsCorrectly() throws Exception {
        // User's real trimasik-kasoti.xlsx has no Present Divas column — subjects start at col 6.
        MockMultipartFile file = createTrimasikExcelWithoutAttendedDays("20", "18", "15");

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

        AnnualExamResult result = getSavedResult();

        assertEquals(3, result.getSubjects().size());
        assertSubject(result.getSubjects().get(0), "Gujarati", 20, 25, "PRESENT");
        assertSubject(result.getSubjects().get(1), "Mathematics", 18, 25, "PRESENT");
        assertSubject(result.getSubjects().get(2), "English", 15, 25, "PRESENT");
        // No attended days column — attended days should be null.
        assertNull(result.getAttendedDays());
    }

    @Test
    void annualUpload_withGradeColumnInExcel_shouldIgnoreExcelGradeAndCalculateFromPolicy()
            throws Exception {
        // Excel has Grade column (step=3) but backend must ignore it and compute from grade policy.
        // Default policy: A>=80%, B>=65%, C>=50%, D>=35%.
        // 20/25=80% -> A, 40/50=80% -> A, 18/25=72% -> B.
        // Excel provides bogus grade values "X", "Y", "Z" which must be completely ignored.
        MockMultipartFile file =
                createAnnualExcel("25", "20", "X", "50", "40", "Y", "25", "18", "Z");

        service.processExcelUpload(schoolId, file, "250", ExamResultService.ANNUAL);

        AnnualExamResult result = getSavedResult();
        // Grades are calculated from policy, NOT copied from Excel.
        assertEquals("A", findSubject(result, "Gujarati").getGrade()); // 80%
        assertEquals("A", findSubject(result, "Mathematics").getGrade()); // 80%
        assertEquals("B", findSubject(result, "English").getGrade()); // 72%
    }

    @Test
    void uploadingNewTrimasikResult_shouldDeleteOldTrimasikResults() throws Exception {
        MockMultipartFile file = createTrimasikExcel("20", "18", "15");

        service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);

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

        when(schoolRepository.findByIdAndIsActiveTrue(schoolId)).thenReturn(Optional.of(school));

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
                "25", gujaratiObtained, "25", mathematicsObtained, "25", englishObtained);
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
             * Trimasik format WITH Present Divas column:
             *
             * 0 = GR No., 1 = Student UID, 2 = Standard, 3 = Class, 4 = Name,
             * 5 = Birth Date, 6 = Present Divas
             *
             * Subject groups start at col 7: Gujarati = Max + Obtained
             */

            Row header = sheet.createRow(0);

            header.createCell(0).setCellValue("GR No.");
            header.createCell(1).setCellValue("Student UID");
            header.createCell(2).setCellValue("Standard");
            header.createCell(3).setCellValue("Class");
            header.createCell(4).setCellValue("Name");
            header.createCell(5).setCellValue("Birth Date");
            header.createCell(6).setCellValue("Present Divas");

            header.createCell(7).setCellValue("Gujarati");
            header.createCell(8).setCellValue("Obtained");

            header.createCell(9).setCellValue("Mathematics");
            header.createCell(10).setCellValue("Obtained");

            header.createCell(11).setCellValue("English");
            header.createCell(12).setCellValue("Obtained");

            Row student = sheet.createRow(1);

            student.createCell(0).setCellValue("101");
            student.createCell(1).setCellValue("UID-1");
            student.createCell(2).setCellValue("3");
            student.createCell(3).setCellValue("A");
            student.createCell(4).setCellValue("Test Student");
            student.createCell(5).setCellValue("01/01/2018");
            student.createCell(6).setCellValue("-");

            student.createCell(7).setCellValue(gujaratiMax);
            student.createCell(8).setCellValue(gujaratiObtained);

            student.createCell(9).setCellValue(mathematicsMax);
            student.createCell(10).setCellValue(mathematicsObtained);

            student.createCell(11).setCellValue(englishMax);
            student.createCell(12).setCellValue(englishObtained);

            workbook.write(output);

            return new MockMultipartFile(
                    "file",
                    "trimasik-result.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    output.toByteArray());
        }
    }

    /**
     * Trimasik format WITHOUT Present Divas column (subjects start directly at col 6). This matches
     * the user's real trimasik-kasoti.xlsx.
     */
    private MockMultipartFile createTrimasikExcelWithoutAttendedDays(
            String gujaratiObtained, String mathematicsObtained, String englishObtained)
            throws Exception {

        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Results");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("GR No.");
            header.createCell(1).setCellValue("Student UID");
            header.createCell(2).setCellValue("Standard");
            header.createCell(3).setCellValue("Class");
            header.createCell(4).setCellValue("Name");
            header.createCell(5).setCellValue("Birth Date");
            // No Present Divas — subjects start at col 6:
            header.createCell(6).setCellValue("Gujarati");
            header.createCell(7).setCellValue("Obtained");
            header.createCell(8).setCellValue("Mathematics");
            header.createCell(9).setCellValue("Obtained");
            header.createCell(10).setCellValue("English");
            header.createCell(11).setCellValue("Obtained");

            Row student = sheet.createRow(1);
            student.createCell(0).setCellValue("101");
            student.createCell(1).setCellValue("UID-1");
            student.createCell(2).setCellValue("3");
            student.createCell(3).setCellValue("A");
            student.createCell(4).setCellValue("Test Student");
            student.createCell(5).setCellValue("01/01/2018");
            // col 6 onwards: subject data directly
            student.createCell(6).setCellValue("25");
            student.createCell(7).setCellValue(gujaratiObtained);
            student.createCell(8).setCellValue("25");
            student.createCell(9).setCellValue(mathematicsObtained);
            student.createCell(10).setCellValue("25");
            student.createCell(11).setCellValue(englishObtained);

            workbook.write(output);

            return new MockMultipartFile(
                    "file",
                    "trimasik-no-attendance.xlsx",
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
        return createAnnualExcelWithAttended(
                "233",
                gujaratiMax,
                gujaratiObtained,
                gujaratiGrade,
                mathematicsMax,
                mathematicsObtained,
                mathematicsGrade,
                englishMax,
                englishObtained,
                englishGrade);
    }

    private MockMultipartFile createAnnualExcelWithAttended(
            String attendedDays,
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
             * 1 = Student UID
             * 2 = Standard
             * 3 = Class
             * 4 = Name
             * 5 = Birth Date
             * 6 = Present Divas
             *
             * Subject groups:
             * Gujarati    = Max + Obtained + Grade
             * Mathematics = Max + Obtained + Grade
             * English     = Max + Obtained + Grade
             */

            Row header = sheet.createRow(0);

            header.createCell(0).setCellValue("GR No.");
            header.createCell(1).setCellValue("Student UID");
            header.createCell(2).setCellValue("Standard");
            header.createCell(3).setCellValue("Class");
            header.createCell(4).setCellValue("Name");
            header.createCell(5).setCellValue("Birth Date");
            header.createCell(6).setCellValue("Present Divas");

            header.createCell(7).setCellValue("Gujarati");
            header.createCell(8).setCellValue("Obtained");
            header.createCell(9).setCellValue("Grade");

            header.createCell(10).setCellValue("Mathematics");
            header.createCell(11).setCellValue("Obtained");
            header.createCell(12).setCellValue("Grade");

            header.createCell(13).setCellValue("English");
            header.createCell(14).setCellValue("Obtained");
            header.createCell(15).setCellValue("Grade");

            Row student = sheet.createRow(1);

            student.createCell(0).setCellValue("101");
            student.createCell(1).setCellValue("UID-1");
            student.createCell(2).setCellValue("3");
            student.createCell(3).setCellValue("A");
            student.createCell(4).setCellValue("Test Student");
            student.createCell(5).setCellValue("01/01/2018");
            student.createCell(6).setCellValue(attendedDays);

            student.createCell(7).setCellValue(gujaratiMax);
            student.createCell(8).setCellValue(gujaratiObtained);
            student.createCell(9).setCellValue(gujaratiGrade);

            student.createCell(10).setCellValue(mathematicsMax);
            student.createCell(11).setCellValue(mathematicsObtained);
            student.createCell(12).setCellValue(mathematicsGrade);

            student.createCell(13).setCellValue(englishMax);
            student.createCell(14).setCellValue(englishObtained);
            student.createCell(15).setCellValue(englishGrade);

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

    private AnnualExamResultSubject findSubject(AnnualExamResult result, String subjectName) {

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

    @Test
    void getResult_shouldReturnResultWhenFound() {
        AnnualExamResult entity = new AnnualExamResult();
        entity.setId(UUID.randomUUID());
        entity.setSchoolId(schoolId);
        entity.setResultType(ExamResultService.ANNUAL);
        entity.setStandard("3");
        entity.setStudentClass("A");
        entity.setRollNumber(1);
        entity.setStudentName("Test Student");

        when(resultRepository.findResult(schoolId, ExamResultService.ANNUAL, "3", "A", 1))
                .thenReturn(Optional.of(entity));

        var response = service.getResult(schoolId, "3", "A", 1, ExamResultService.ANNUAL);

        assertNotNull(response);
        assertEquals("Test Student", response.studentName());
        assertEquals(schoolId, response.schoolId());
    }

    @Test
    void getResult_shouldThrowWhenNotFound() {
        when(resultRepository.findResult(schoolId, ExamResultService.ANNUAL, "3", "A", 99))
                .thenReturn(Optional.empty());

        DomainException ex =
                assertThrows(
                        DomainException.class,
                        () -> service.getResult(schoolId, "3", "A", 99, ExamResultService.ANNUAL));
        assertEquals("result_not_found", ex.getCode());
    }

    @Test
    void getResult_shouldThrowWhenSchoolInactive() {
        UUID unknownSchoolId = UUID.randomUUID();
        when(schoolRepository.findByIdAndIsActiveTrue(unknownSchoolId))
                .thenReturn(Optional.empty());

        DomainException ex =
                assertThrows(
                        DomainException.class,
                        () ->
                                service.getResult(
                                        unknownSchoolId, "3", "A", 1, ExamResultService.ANNUAL));
        assertEquals("school_not_found", ex.getCode());
    }

    @Test
    void clearResults_shouldDeleteOnlyForSpecifiedSchool() {
        service.clearResults(schoolId, ExamResultService.ANNUAL);

        verify(resultRepository).deleteBySchoolIdAndResultType(schoolId, ExamResultService.ANNUAL);
    }

    @Test
    void
            resultXlsxTemplateFormat_shouldImportBothAnnualAndEkamKasoti_withHyphenAttendedDaysAndReorderedSubjects()
                    throws Exception {
        /*
         * Reordered synthetic workbook matching exact 25 columns of tmp/result.xlsx:
         * GR No. | Student UID | Standard | Class | Name | Birth Date | Attended Days |
         * Maths  | Obtained | Gujarati | Obtained | Science | Obtained | Hindi | Obtained |
         * English | Obtained | SS | Obtained | Sanskrit | Obtained | PD | Obtained | Evs | Obtained
         * Notice: Maths is ordered before Gujarati to assert subject-to-column mapping is not positional.
         * Maths header has trailing space ('Maths ').
         * Attended Days contains '-'.
         */
        try (XSSFWorkbook workbook = new XSSFWorkbook();
                ByteArrayOutputStream output = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Results");
            Row header = sheet.createRow(0);

            header.createCell(0).setCellValue("GR No.");
            header.createCell(1).setCellValue("Student UID");
            header.createCell(2).setCellValue("Standard");
            header.createCell(3).setCellValue("Class");
            header.createCell(4).setCellValue("Name");
            header.createCell(5).setCellValue("Birth Date");
            header.createCell(6).setCellValue("Attended Days");

            // Subject 1 in file: Maths (with trailing space)
            header.createCell(7).setCellValue("Maths ");
            header.createCell(8).setCellValue("Obtained");
            // Subject 2 in file: Gujarati
            header.createCell(9).setCellValue("Gujarati");
            header.createCell(10).setCellValue("Obtained");
            // Subject 3 in file: English
            header.createCell(11).setCellValue("English");
            header.createCell(12).setCellValue("Obtained");

            // Remaining subjects in 25-column layout (unconfigured for standard 3, blank obtained)
            String[] remaining = {"Science", "Hindi", "SS", "Sanskrit", "PD", "Evs"};
            int col = 13;
            for (String sub : remaining) {
                header.createCell(col++).setCellValue(sub);
                header.createCell(col++).setCellValue("Obtained");
            }

            // Student row
            Row student = sheet.createRow(1);
            student.createCell(0).setCellValue("4110");
            student.createCell(1).setCellValue("999999999999999999"); // UID must be ignored
            student.createCell(2).setCellValue("3");
            student.createCell(3).setCellValue("A");
            student.createCell(4).setCellValue("Synthetic Student");
            student.createCell(5).setCellValue("04.12.2015");
            student.createCell(6).setCellValue("-"); // Attended Days is '-'

            // Maths marks: max 40, obtained 35
            student.createCell(7).setCellValue("40");
            student.createCell(8).setCellValue("35");
            // Gujarati marks: max 40, obtained 30
            student.createCell(9).setCellValue("40");
            student.createCell(10).setCellValue("30");
            // English marks: max 40, obtained 25
            student.createCell(11).setCellValue("40");
            student.createCell(12).setCellValue("25");

            // Blank obtained cells for unconfigured subjects (like sample template)
            for (int c = 13; c < 25; c += 2) {
                student.createCell(c).setCellValue("40");
                student.createCell(c + 1).setCellValue("");
            }

            workbook.write(output);
            MockMultipartFile file =
                    new MockMultipartFile(
                            "file",
                            "synthetic-result.xlsx",
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                            output.toByteArray());

            // 1. Test Annual Exam upload
            service.processExcelUpload(schoolId, file, "200", ExamResultService.ANNUAL);
            AnnualExamResult annualResult = getSavedResult();

            assertEquals("-", annualResult.getAttendedDays());
            assertEquals("200", annualResult.getTotalWorkingDays());
            assertEquals("3", annualResult.getStandard());
            assertEquals("A", annualResult.getStudentClass());
            assertEquals("Synthetic Student", annualResult.getStudentName());
            assertEquals(1, annualResult.getRollNumber());
            assertEquals("4110", annualResult.getGeneralRegisterNumber());

            // Subject mapping must accurately map marks regardless of header order
            AnnualExamResultSubject maths = findSubject(annualResult, "Mathematics");
            assertEquals(35, maths.getObtainedMarks());
            assertEquals(40, maths.getMaximumMarks());
            assertEquals("A", maths.getGrade()); // 35/40 = 87.5% >= 80%

            AnnualExamResultSubject gujarati = findSubject(annualResult, "Gujarati");
            assertEquals(30, gujarati.getObtainedMarks());
            assertEquals(40, gujarati.getMaximumMarks());
            assertEquals("B", gujarati.getGrade()); // 30/40 = 75% >= 65%

            AnnualExamResultSubject english = findSubject(annualResult, "English");
            assertEquals(25, english.getObtainedMarks());
            assertEquals(40, english.getMaximumMarks());
            assertEquals("C", english.getGrade()); // 25/40 = 62.5% >= 50%

            assertEquals(120, annualResult.getTotalMarks());
            assertEquals(90, annualResult.getObtainedMarks());

            // 2. Test same file structure for Ekam Kasoti upload
            org.mockito.Mockito.reset(resultRepository);
            service.processExcelUpload(schoolId, file, null, ExamResultService.EKAM_KASOTI);
            AnnualExamResult ekamResult = getSavedResult();

            assertEquals("-", ekamResult.getAttendedDays());
            assertNull(ekamResult.getTotalWorkingDays());
            AnnualExamResultSubject ekamMaths = findSubject(ekamResult, "Mathematics");
            assertEquals(35, ekamMaths.getObtainedMarks());
            assertNull(ekamMaths.getGrade()); // Ekam Kasoti has no grades
        }
    }
}
