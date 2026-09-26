# Result Import Contract and Safety

## Intended responsibilities

The principal creates an assessment, obtains or provides the approved format, uploads a workbook, reviews validation and a preview, explicitly confirms a transactional import, and then separately publishes draft results. Apache POI is the planned parser once the contract exists.

## Import principles

- Accept uploads only from an authorized principal and under a documented server-side size/type policy.
- Keep temporary source files private, short-lived, and cleaned after success/failure according to the implemented policy.
- Validate before mutation; show a human-readable preview and errors before confirmation.
- Confirmation performs a transactional import where possible. On failure, do not leave partial result records; record safe audit details and provide rollback/recovery guidance.
- Detect duplicate imports or overlapping result data using the eventual approved business key; require an explicit, auditable resolution rather than silently overwriting data.
- Imported results are draft. Only explicit principal publication makes an assessment available to public individual lookup.
- Tests will use owner-provided representative workbooks after the real contract is documented.

## Tri-masik Ekam Kasoti — Standard 3 Workbook Contract

This contract was observed locally from the ignored source workbook. It deliberately excludes student names, identifiers, marks, and other real student data.

### Observed

- The input is one visible Office Open XML workbook (`.xlsx`) with one sheet named `Sheet 1`.
- The effective populated content is columns `A:H`, rows 1–37. The workbook reports a larger used-row extent because of trailing formatting; those trailing rows contain no values and are not student records.
- `A1:H1` is merged. Its Gujarati title identifies Standard 3, the first trimester/Ekam Kasoti assessment, and academic year `2026-27`.
- Row 2 is the sole header row: `AadhaarUID`, `StudentName`, `Attendance`, `GUJARATI (40)`, `MATHS (40)`, `EVS (40)`, `ENGLISH (40)`, and `TOTAL (160)`.
- Student rows follow immediately after the header and end before two summary rows. Attendance is a one-character text value. Subject marks are numeric cells except for the observed absent marker `-`. `TOTAL` is a `SUM` formula across the four subject columns.
- The source contains 33 populated student rows. Blank rows do not separate students.
- `A36:C36` and `A37:C37` are merged summary labels. The first reports class averages for the subject columns; the second reports class percentages. These are aggregate presentation fields, not per-student marks.
- No row/column is hidden. There are no additional sheets. No dates, grades, pass/fail result, rank, roll number, PIN, or maximum marks separate from the four subject headers were observed.

### Required for a future parser

- A single visible sheet with the exact row-2 headers, the merged title range, four subject columns carrying a parenthesized maximum, and formula totals in column H.
- A title that resolves to Standard 3 and the observed assessment/year context supplied by the principal-created assessment.
- Numeric subject marks from zero through the maximum declared in each subject header, or the observed `-` absent marker. Formula totals must be recomputed from subject marks rather than trusted as a stored source value.
- Summary rows must be excluded from student parsing; trailing styled rows must not be interpreted as data.

### Optional

- The `TOTAL` formula is useful for preview cross-checking only. It is not persisted because total and percentage can be derived from imported subject marks and configured maxima.
- Attendance can be preserved in preview diagnostics, but there is no approved persistence field for it in V1.

### Derived

- Four assessment subjects can map to the existing generic assessment model with maximum mark 40 each and an assessment maximum of 160.
- `EKAM_KASOTI` remains the appropriate existing generic type. The human-readable assessment name must retain the workbook title’s Tri-masik/Ekam Kasoti wording.

### Unknown or pending — import is blocked

- The only workbook identity column is `AadhaarUID`, a government identifier. The portal privacy model prohibits storing it. The workbook contains no roll number, no alternate approved student key, and no result PIN.
- There is therefore no safe mapping from a workbook row to the required `students.roll_number`, nor a safe public lookup workflow. Hashing or retaining the Aadhaar identifier would still create an unapproved government-ID handling scheme.
- The owner must approve one of: (1) a workbook revision containing a school-controlled non-government roll number; or (2) a separate, documented pre-import enrollment/mapping workflow that supplies roll numbers and a secure PIN-distribution process. Until then, preview, confirmation, persistence, publication, and public lookup are intentionally not implemented.
- Other exam-result workbook formats, exam timetable, and school timetable remain pending real samples or requirements.

### Superseding implementation note

The Standard 3 Ekam Kasoti upload now uses the permitted separate enrolment mapping workflow: before upload, the principal enrols every student in the current academic year with a unique numeric, school-controlled roll number. Rows are matched only by exact normalized student name. The importer never reads, logs, hashes, or stores the `AadhaarUID` cell, rejects duplicate names rather than guessing, and replaces only the current year's Ekam Kasoti results after the complete workbook validates. The public result page remains annual-result-only until an Ekam Kasoti release and lookup flow is approved.
