# Annual Exam Result System — Architecture & Workflow

This document outlines the complete technical architecture, database schema, Excel parsing mechanics, and frontend presentation workflow for the **Annual Exam Result System (વાર્ષિક પરીક્ષા પરિણામ)** in the Rorfost School Platform.

---

## 1. Overview & Multi-Tenant Architecture

The Annual Exam Result System allows school administrators to upload an Excel sheet (`.xlsx`) containing exam performance data for students across Standards 1 to 8. Students and parents can instantly view and print their official annual marksheet by selecting their **Standard (ધોરણ)** and entering their **Roll Number (રોલ નંબર)**.

### As-Is vs. Target Multi-Tenant Flow

- **Current State (As-Is):** Single school deployment. The backend resolves the active school via `SchoolRepository.findFirstByIsActiveTrueOrderByCreatedAtAsc()`.
- **Target State (Sprint 09):** The upload and public lookup flows are strictly isolated to `TenantContext.schoolId`. An admin from School A can only upload or clear results for School A; students querying results on School B's portal only see School B's results.

```
┌─────────────────────────┐       Upload Excel (.xlsx)       ┌──────────────────────────────────┐
│   Admin Panel Upload    │ ───────────────────────────────> │  ExamResultService (Spring Boot) │
│ (/admin/assessments)    │  + Total Working Days (હાજર દિવસ)│  - Scoped to TenantContext       │
└─────────────────────────┘                                  │  - Replaces tenant result type   │
                                                             │  - Parses Apache POI Rows        │
                                                             └────────────────┬─────────────────┘
                                                                              │
                                                                              ▼
┌─────────────────────────┐      Query (Standard + Roll)     ┌──────────────────────────────────┐
│  Public Results Page    │ <─────────────────────────────── │  AnnualExamResult Repository     │
│  (/student/results)     │  Returns ExamResultResponse JSON │  (PostgreSQL scoped to school_id)│
└────────────┬────────────┘  + Cache-Control: no-store       └──────────────────────────────────┘
             │
             ▼
┌─────────────────────────┐
│  ExamResultViewer.tsx   │
│  - Dynamic School Info  │
│  - A4 Printable Layout  │
└─────────────────────────┘
```

---

## 2. Academic Setup & Database Schema

### 2.1 Database Models

#### `annual_exam_results` (Main Student Record)

| Column Name               | Type           | Description                                        |
| :------------------------ | :------------- | :------------------------------------------------- |
| `id`                      | `UUID` (PK)    | Unique primary key                                 |
| `school_id`               | `UUID` (FK)    | Reference to active school tenant                  |
| `standard`                | `VARCHAR(32)`  | Student Standard (e.g. `1`, `2`, `3`, ..., `8`)    |
| `roll_number`             | `INT`          | Roll number assigned sequentially per standard     |
| `student_name`            | `VARCHAR(255)` | Student's full name in Gujarati                    |
| `general_register_number` | `VARCHAR(64)`  | G.R. Number (જી.આર. નં)                            |
| `birth_date`              | `VARCHAR(64)`  | Student birth date (જન્મ તારીખ)                    |
| `total_working_days`      | `INT`          | Total school working days (કુલ કાર્ય દિવસ)         |
| `attended_days`           | `INT`          | Student attended days (હાજર દિવસ)                  |
| `total_marks`             | `INT`          | Maximum total marks across all applicable subjects |
| `obtained_marks`          | `INT`          | Total marks obtained by student                    |
| `percentage`              | `DECIMAL(5,2)` | Calculated overall percentage                      |
| `overall_grade`           | `VARCHAR(8)`   | Calculated overall grade (`A`, `B`, `C`, `D`, `E`) |

#### `annual_exam_result_subjects` (Subject Marks Detail)

| Column Name             | Type           | Description                                     |
| :---------------------- | :------------- | :---------------------------------------------- |
| `id`                    | `UUID` (PK)    | Unique primary key                              |
| `annual_exam_result_id` | `UUID` (FK)    | Parent result foreign key                       |
| `subject_name`          | `VARCHAR(128)` | Gujarati Subject Name (e.g., `ગુજરાતી`, `ગણિત`) |
| `maximum_marks`         | `INT`          | Maximum subject marks (e.g., 200, 400)          |
| `obtained_marks`        | `INT`          | Marks obtained in subject                       |
| `grade`                 | `VARCHAR(8)`   | Subject grade (`A`, `B`, `C`, etc.)             |
| `sort_order`            | `INT`          | Order of subject in marksheet table             |

---

## 3. Excel Sheet Structure & Column Mapping

The system accepts standard primary school Excel exports (`exam-result.xlsx`). Row 0 contains header names, and data rows start from Row 1.

### 3.1 Exam Result Excel Column Layout Index

There is no Sr.No. column. Indices below are 0-based.

| Column Index | Column Name in Excel             | Field Mapping in Code   | Max Marks |
| :----------: | :------------------------------- | :---------------------- | :-------: |
|     `0`      | G.R. No. / જી.આર. નં             | `generalRegisterNumber` |     —     |
|     `1`      | Standard / ધોરણ                  | `standard`              |     —     |
|     `2`      | Student Name / વિદ્યાર્થીનું નામ | `studentName`           |     —     |
|     `3`      | Birth Date / જન્મ તારીખ          | `birthDate`             |     —     |
|     `4`      | Attended Days / હાજર દિવસ        | `attendedDays`          |     —     |
|   `5`, `6`   | Gujarati Marks & Grade           | `ગુજરાતી`               |    200    |
|   `7`, `8`   | Maths Marks & Grade              | `ગણિત`                  |    200    |
|  `9`, `10`   | Science Marks & Grade            | `વિજ્ઞાન`               |    200    |
|  `11`, `12`  | Hindi Marks & Grade              | `હિન્દી`                |    200    |
|  `13`, `14`  | English Marks & Grade            | `અંગ્રેજી`              |    200    |
|  `15`, `16`  | Social Science Marks & Grade     | `સામાજીક વિજ્ઞાન`       |    200    |
|  `17`, `18`  | Sanskrit Marks & Grade           | `સંસ્કૃત`               |    200    |
|  `19`, `20`  | Personality Dev Marks & Grade    | `વ્યક્તિત્વ વિકાસ`      |    400    |
|  `21`, `22`  | Environmental Marks & Grade      | `પર્યાવરણ`              |    200    |

### 3.2 Ekam Kasoti Excel Column Layout Index

Same as Exam Result after dropping Attended Days; subject columns shift left by one, starting at index 4.

| Column Index | Column Name in Excel     | Field Mapping in Code   |
| :----------: | :----------------------- | :---------------------- |
|     `0`      | G.R. No.                 | `generalRegisterNumber` |
|     `1`      | Standard                 | `standard`              |
|     `2`      | Student Name             | `studentName`           |
|     `3`      | Birth Date               | `birthDate`             |
|  `4` onward  | Subject mark/grade pairs | configured subjects     |

---

## 4. Backend Processing Mechanics (`ExamResultService.java`)

When an administrator uploads an Excel file via `POST /api/v1/admin/exam-results/upload`:

1. **Transactional Re-upload Handling:**
   To allow re-uploads without duplicate key errors, the backend deletes prior results within the active school scope:
   ```java
   resultRepository.deleteBySchoolIdAndResultType(school.getId(), resultType);
   ```

2. **Sequential Roll Number Assignment:**
   Roll numbers are assigned sequentially per standard based on row order:
   ```java
   int roll = standardRollCounts.getOrDefault(standard, 0) + 1;
   standardRollCounts.put(standard, roll);
   result.setRollNumber(roll);
   ```

3. **Grade and Percentage Formula:**
   $$\text{Percentage } (p) = \frac{\text{Total Obtained Marks} \times 100}{\text{Total Maximum Marks}}$$
   - $p \ge 80\%$: Grade **A**
   - $65\% \le p < 80\%$: Grade **B**
   - $50\% \le p < 65\%$: Grade **C**
   - $35\% \le p < 50\%$: Grade **D**
   - $p < 35\%$: Grade **E**

---

## 5. Public Frontend Lookup & Presentation Flow

### 5.1 Student Search (`ResultsInfoPage.tsx`)
1. Student selects **Standard (ધોરણ)** (1 to 8) and enters **Roll Number (રોલ નંબર)**.
2. Form triggers API request `GET /api/v1/public/exam-results?standard={std}&rollNumber={roll}&resultType={type}`.
3. Upon response, `ExamResultViewer` renders the marksheet.

### 5.2 Dynamic Marksheet Formatting (`ExamResultViewer.tsx`)
- **Header:** Dynamic School Logo, Official School Name, Subtitle, and Title Banner from the active school's profile.
- **Student Details Grid:** Gujarati labels for student name, standard, G.R. number, birth date, roll number, and attended vs. total working days.
- **Marks Table:** English numerals for mark figures; Gujarati numerals for standard/serial numbers.
- **Signatures & Seal:** Dynamic Class Teacher & Principal signatures and official school seal where configured.
- **A4 Print:** Clean single-page printout via `@media print` rules hiding navigation and controls.

---

## 6. API Endpoints Summary

| Method | Endpoint | Access | Purpose |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/public/exam-results` | Public | Fetch result by standard, rollNumber, and resultType (scoped to tenant) |
| `POST` | `/api/v1/admin/exam-results/upload` | Admin (`ROLE_PRINCIPAL`) + CSRF | Upload Excel file with totalWorkingDays and resultType |
| `DELETE` | `/api/v1/admin/exam-results` | Admin (`ROLE_PRINCIPAL`) + CSRF | Clear uploaded results for the specified resultType |
