# Annual Exam Result System — End-to-End Architecture & Workflow

This document outlines the complete technical architecture, database setup, Excel parsing mechanics, and frontend presentation workflow for the **Annual Exam Result System (વાર્ષિક પરીક્ષા પરિણામ)** in the School Platform.

---

## 1. Overview & Architecture

The Annual Exam Result System allows school administrators to upload a single consolidated Excel sheet (`.xlsx`) containing exam performance data for students across all standards (Std 1 to 8). Students and parents can instantly view and print their official annual marksheet by providing their **Standard (ધોરણ)** and **Roll Number (રોલ નંબર)**.

```
┌─────────────────────────┐       Upload Excel (.xlsx)       ┌──────────────────────────────────┐
│   Admin Panel Upload    │ ───────────────────────────────> │  ExamResultService (Spring Boot) │
│ (/admin/assessments)    │  + Total Working Days (હાજર દિવસ)│  - Clears previous year records  │
└─────────────────────────┘                                  │  - Parses Apache POI Rows        │
                                                             └────────────────┬─────────────────┘
                                                                              │
                                                                              ▼
┌─────────────────────────┐      Query (Standard + Roll)     ┌──────────────────────────────────┐
│  Public Results Page    │ <─────────────────────────────── │  AnnualExamResult Repository     │
│  (/student/results)     │  Returns ExamResultResponse JSON │  (PostgreSQL / H2 Database)      │
└────────────┬────────────┘                                  └──────────────────────────────────┘
             │
             ▼
┌─────────────────────────┐
│  ExamResultViewer.tsx   │
│  - Standard Marksheet   │
│  - A4 Printable Layout  │
└─────────────────────────┘
```

---

## 2. Academic Setup & Database Schema

### 2.1 Database Models

#### `annual_exam_results` (Main Student Record)
| Column Name | Type | Description |
| :--- | :--- | :--- |
| `id` | `UUID` (PK) | Unique primary key |
| `school_id` | `UUID` (FK) | Reference to active school |
| `academic_year_id` | `UUID` (FK) | Reference to active academic year (e.g. 2025-26) |
| `standard` | `VARCHAR(32)` | Student Standard (e.g. `1`, `2`, `3`, ..., `8`) |
| `roll_number` | `INT` | Roll number assigned sequentially per standard |
| `student_name` | `VARCHAR(255)` | Student's full name in Gujarati |
| `general_register_number`| `VARCHAR(64)`| G.R. Number (જી.આર. નં) |
| `birth_date` | `VARCHAR(64)` | Student birth date (જન્મ તારીખ) |
| `total_working_days` | `INT` | Total school working days (કુલ કાર્ય દિવસ) |
| `attended_days` | `INT` | Student attended days (હાજર દિવસ) |
| `total_marks` | `INT` | Maximum total marks across all applicable subjects |
| `obtained_marks` | `INT` | Total marks obtained by student |
| `percentage` | `DECIMAL(5,2)`| Calculated overall percentage |
| `overall_grade` | `VARCHAR(8)` | Calculated overall grade (`A`, `B`, `C`, `D`, `E`) |

#### `annual_exam_result_subjects` (Subject Marks Detail)
| Column Name | Type | Description |
| :--- | :--- | :--- |
| `id` | `UUID` (PK) | Unique primary key |
| `annual_exam_result_id`| `UUID` (FK)| Parent result foreign key |
| `subject_name` | `VARCHAR(128)`| Gujarati Subject Name (e.g., `ગુજરાતી`, `ગણિત`) |
| `maximum_marks` | `INT` | Maximum subject marks (e.g., 200, 400) |
| `obtained_marks` | `INT` | Marks obtained in subject |
| `grade` | `VARCHAR(8)` | Subject grade (`A`, `B`, `C`, etc.) |
| `sort_order` | `INT` | Order of subject in marksheet table |

---

## 3. Excel Sheet Structure & Column Mapping

The system accepts standard primary school Excel exports (`exam-result.xlsx`). Row 0 contains header names, and data rows start from Row 1.

### 3.1 Excel Column Layout Index

| Column Index | Column Name in Excel | Field Mapping in Code | Max Marks |
| :---: | :--- | :--- | :---: |
| `0` | SR / ક્રમ | Row index | — |
| `1` | G.R. No. / જી.આર. નં | `generalRegisterNumber` | — |
| `2` | Standard / ધોરણ | `standard` | — |
| `3` | Student Name / વિદ્યાર્થીનું નામ | `studentName` | — |
| `4` | Birth Date / જન્મ તારીખ | `birthDate` | — |
| `5` | Attended Days / હાજરી | `attendedDays` | — |
| `6`, `7` | Gujarati Marks & Grade | `ગુજરાતી` | 200 |
| `8`, `9` | Maths Marks & Grade | `ગણિત` | 200 |
| `10`, `11` | Science Marks & Grade | `વિજ્ઞાન` | 200 |
| `12`, `13` | Hindi Marks & Grade | `હિન્દી` | 200 |
| `14`, `15` | English Marks & Grade | `અંગ્રેજી` | 200 |
| `16`, `17` | Social Science Marks & Grade | `સામાજીક વિજ્ઞાન` | 200 |
| `18`, `19` | Sanskrit Marks & Grade | `સંસ્કૃત` | 200 |
| `20`, `21` | Personality Dev Marks & Grade | `વ્યક્તિત્વ વિકાસ` | 400 |
| `22`, `23` | Environmental Marks & Grade | `પર્યાવરણ` | 200 |

> **Note on Standard-Subject Dynamic Handling:**
> Lower standards (e.g. Std 1-3) may not have marks for subjects like English or Sanskrit in the Excel sheet. The code dynamically checks if the marks cell is non-empty before adding the subject. Empty subject cells are automatically skipped and omitted from both the total max marks calculation and the student's result card.

---

## 4. Backend Processing Mechanics (`ExamResultService.java`)

When an administrator uploads an Excel file via `POST /api/v1/admin/exam-results/upload`:

1. **Transactional Re-upload Handling:**
   To allow easy re-uploads without duplicate key errors, the backend executes a custom batch query:
   ```java
   resultRepository.deleteBySchoolIdAndAcademicYearIdAndResultType(
       school.getId(), academicYear.getId(), resultType);
   ```

2. **Sequential Roll Number Assignment:**
   Roll numbers are automatically generated per standard in sequence based on row order:
   ```java
   int roll = standardRollCounts.getOrDefault(standard, 0) + 1;
   standardRollCounts.put(standard, roll);
   result.setRollNumber(roll);
   ```

3. **Subject Extraction & Grade Calculation:**
   Each subject pair (Marks Cell, Grade Cell) is extracted safely:
   ```java
   private int addSubject(AnnualExamResult result, String name, int max, Cell marksCell, Cell gradeCell, int sortOrder) {
       String marks = getCellString(marksCell);
       String grade = getCellString(gradeCell);
       if (marks == null || marks.isBlank()) return 0; // Skip empty subject
       
       AnnualExamResultSubject sub = new AnnualExamResultSubject();
       sub.setSubjectName(name);
       sub.setMaximumMarks(max);
       sub.setSortOrder(sortOrder);
       sub.setGrade(grade);
       sub.setObtainedMarks((int) Double.parseDouble(marks));
       result.addSubject(sub);
       return max;
   }
   ```

4. **Overall Grade & Percentage Formula:**
   $$\text{Percentage } (p) = \frac{\text{Total Obtained Marks} \times 100}{\text{Total Maximum Marks}}$$

   Grade evaluation criteria:
   - **$p \ge 80\%$**: Grade **A**
   - **$65\% \le p < 80\%$**: Grade **B**
   - **$50\% \le p < 65\%$**: Grade **C**
   - **$35\% \le p < 50\%$**: Grade **D**
   - **$p < 35\%$**: Grade **E**

---

## 5. Public Frontend Lookup & Presentation Flow

### 5.1 Student Search (`ResultsInfoPage.tsx`)
1. Student selects **Standard (ધોરણ)** (1 to 8) and enters **Roll Number (રોલ નંબર)**.
2. Form triggers API request `GET /api/v1/public/exam-results?standard={std}&rollNumber={roll}`.
3. Upon response, the `ExamResultViewer` component mounts and renders the marksheet.

### 5.2 Marksheet Formatting & Presentation (`ExamResultViewer.tsx`)
The marksheet follows the official Primary Education Board layout:

- **School Header:** School Logo, Official Name, Subtitle (`તા. સમી, જિ. પાટણ`), Title Banner (`પરિણામ પત્રક : ૨૦૨૫-૨૬`).
- **Student Details Grid:**
  - **વિદ્યાર્થીનું નામ (Name):** Student Full Name
  - **ધોરણ (Standard):** Gujarati Number (e.g. `૧`, `૫`, `૮`)
  - **જી.આર. નંબર (G.R. No):** General Register Number
  - **જન્મ તારીખ (Birth Date):** Date of Birth
  - **રોલ નંબર (Roll No):** Roll Number
  - **કાર્ય દિવસ (Working Days):** Total Working Days vs Attended Days (`૧૮૦ માંથી ૧૬૫`)
- **Marks Table:**
  - **Col 1:** Serial No (ક્રમ)
  - **Col 2:** Subject Name (વિષય)
  - **Col 3:** Max Marks (કુલ ગુણ - English numerals)
  - **Col 4:** Obtained Marks (મેળવેલ ગુણ - English numerals)
  - **Col 5:** Grade (ગ્રેડ)
  - **Col 6:** Subject Remarks (નોંધ)
- **Footer Summary:**
  - **મેળવેલ કુલ ગુણ / ગ્રેડ (Total Obtained / Grade):** Summary row with Total Max Marks, Total Obtained Marks, Overall Grade, and Percentage (`%`).
  - **સહી (Signatures):** Class Teacher Signature (`વર્ગ શિક્ષકની સહી`) & Principal Signature (`આચાર્યની સહી`).
  - **Vacation Reopening Date Note:** Standard primary school announcement text.

### 5.3 Number Localization Rules
- **Result Mark Values (Numbers in Marksheet Table):** Formatted in **English (Western Arabic) numerals** (e.g. `200`, `154`, `85.50%`) as requested.
- **Structural Text & Standard Headers:** Formatted in **Gujarati text and numerals** (`ધોરણ ૫`, `ક્રમ ૧`).

### 5.4 A4 Print Optimization
- Clicking **Print Result (પ્રિન્ટ પરિણામ)** calls `window.print()`.
- `@media print` rules hide sidebars, headers, footers, and background UI elements, isolating `#exam-result-print` into a crisp, single A4 page printout.

---

## 6. API Endpoints Summary

| Method | Endpoint | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/public/exam-results` | Public (`permitAll`) | Fetch result by `standard`, `rollNumber`, and `resultType` |
| `POST` | `/api/v1/admin/exam-results/upload` | Admin (`ROLE_PRINCIPAL`) | Upload the approved shared Excel file with `totalWorkingDays` and `resultType` |

---

## 7. Files Reference List

- **Backend Controller:** `com.rorfost.schoolportal.assessment.api.ExamResultController`
- **Backend Service:** `com.rorfost.schoolportal.assessment.application.ExamResultService`
- **Backend Entity:** `com.rorfost.schoolportal.school.domain.AnnualExamResult`
- **Frontend Search Page:** `frontend/src/pages/ResultsInfoPage.tsx`
- **Frontend Viewer & Print:** `frontend/src/features/public/ExamResultViewer.tsx`
- **Gujarati Number Utility:** `frontend/src/utils/gujarati.ts`
