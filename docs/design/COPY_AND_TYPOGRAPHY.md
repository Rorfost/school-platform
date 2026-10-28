# Gujarati Typography and Localization Standards (Sprint 57)

**Document Version:** 1.0  
**Repository:** https://github.com/Rorfost/school-platform  
**Scope:** Guidelines for Gujarati typographic hierarchy, numeral rendering, line-height compensation, font stack selection, and tenant-safe localized string conventions.

---

## 1. Gujarati-First Public Interface Principles

1. **Natural Gujarati Copy:** All public-facing views (home, notices, materials, results, principal's desk, downloads, gallery, calculators) utilize clear, natural Gujarati terminology.
2. **English Admin Isolation:** Administrative management portals, configuration controls, system alerts, server logs, and developer documentation strictly maintain English labels and messages.
3. **No Unlicensed Bundled Fonts:** Rely exclusively on standardized system and platform font fallbacks without shipping multi-megabyte unlicensed binary `.ttf` or `.woff2` files.
4. **Resilient Numeral Rendering:** All public-facing numbers (years, dates, roll numbers, percentages, DISE codes) support conversion to standard Gujarati numerals (`૦-૯`) using `toGujaratiNumber()`, preserving punctuation, whitespace, and alphanumeric prefixes.

---

## 2. Font Stack & System Fallback Hierarchy

The platform defines `--font-sans` in `frontend/src/styles/index.css`:

```css
--font-sans:
    "Noto Sans Gujarati", "Gujarati Sangam MN", "Nirmala UI", "Shruti",
    system-ui, -apple-system, sans-serif;
```

### Fallback Order Rationale:

- **`"Noto Sans Gujarati"`:** Modern standard OpenType Gujarati font widely available on Android, ChromeOS, and modern Linux/web stacks. Accurate conjunct rendering and diacritical marks.
- **`"Gujarati Sangam MN"`:** High-quality native macOS and iOS system font for Gujarati script, pre-installed across Apple platforms.
- **`"Nirmala UI"`:** Native Windows Indic font (Windows 8, 10, 11) supporting complex Indic glyph conjuncts.
- **`"Shruti"`:** Legacy Windows system font (Windows XP, 7) ensuring backward compatibility.
- **`system-ui, -apple-system, sans-serif`:** Native OS fallbacks.

### Copyright & Zero CDN Cost Compliance:

- No paid third-party font subscriptions (e.g., Adobe Typekit, cloud font CDNs).
- No web-font download waterfalls or layout shifts (CLS = 0) caused by late font swapping.

---

## 3. Typographic Constraints & Gujarati Conjunct Handling

### 3.1 Line Height & Matra Accommodation

- Gujarati characters include ascender and descender vowel modifiers (_matras_, e.g., દીર્ઘ-ઈ `ી`, હ્રસ્વ-ઉ `ુ`, દીર્ઘ-ઊ `ૂ`, રેફ `ર્`).
- Standard tight English line heights (`leading-none` or `leading-tight`) clip matras in tables and titles.
- **Rule:** Global body line-height is calibrated to `1.6`, and table row content uses `line-height: 1.5` minimum.

### 3.2 School Title Overflow on Mobile Viewports (320px - 430px)

- Long school titles (e.g., _"શ્રી ઢઢાણા પ્રાથમિક શાળા અને કન્યા શાળા"_) can exceed viewport widths on narrow mobile screens (320px iPhone SE, 390px iPhone 14, 430px Pro Max).
- **Rule:** All header and footer school brand text containers apply `truncate` / `max-w-*` with a native `title="..."` attribute to preserve accessibility and tooltip discovery without breaking header flex bounds or overflowing the viewport horizontally (`overflow-x: hidden`).

---

## 4. Assessment & Result Terminology Conventions

To prevent confusion between Annual Examination and Trimasik periodic assessments across multi-tenant schools, standardized Gujarati terminology is defined in `frontend/src/utils/gujarati.ts`:

| Standard Term (English) | Gujarati Label                             | System Constant                | Context / Usage                                |
| :---------------------- | :----------------------------------------- | :----------------------------- | :--------------------------------------------- |
| Annual Exam             | વાર્ષિક પરીક્ષા                            | `LABELS.annualExam`            | End-of-year comprehensive evaluation marksheet |
| Trimasik Kasoti         | ત્રિમાસિક કસોટી                            | `LABELS.trimasikKasoti`        | Periodic assessment / unit test report         |
| Annual Exam Result      | વાર્ષિક પરીક્ષા પરિણામ                     | `LABELS.annualExamResult`      | Public marksheet search and title              |
| Trimasik Kasoti Result  | ત્રિમાસિક કસોટી પરિણામ                     | `LABELS.trimasikKasotiResult`  | Public unit test search and title              |
| Student Name            | વિદ્યાર્થીનું નામ                          | `LABELS.studentName`           | Marksheet student metadata                     |
| Standard                | ધોરણ                                       | `LABELS.standard`              | Class standard level (1 to 8)                  |
| Class / Section         | વર્ગ                                       | `LABELS.classLabel`            | Section indicator (A, B, etc.)                 |
| Roll Number             | રોલ નંબર                                   | `LABELS.rollNumber`            | Student roll number identifier                 |
| General Register No.    | જ. રજી. નંબર                               | `LABELS.generalRegisterNumber` | Official school register identifier            |
| Birth Date              | જન્મ તારીખ                                 | `LABELS.birthDate`             | Student date of birth                          |
| Total Marks             | કુલ ગુણ                                    | `LABELS.totalMarks`            | Examination subject max marks                  |
| Obtained Marks          | મેળવેલ ગુણ                                 | `LABELS.obtainedMarks`         | Marks scored by student                        |
| Grade                   | ગ્રેડ                                      | `LABELS.grade`                 | Calculated grade band                          |
| Remarks                 | વિષયના સંદર્ભમાં નોંધ                      | `LABELS.remarks`               | Qualitative remarks column                     |
| Loading                 | લોડ થઈ રહ્યું છે...                        | `LABELS.loading`               | Accessible skeleton & loading states           |
| Empty State             | હાલ કોઈ માહિતી ઉપલબ્ધ નથી.                 | `LABELS.empty`                 | Default list empty notification                |
| Error Message           | હમણાં માહિતી મળી શકી નથી. ફરી પ્રયત્ન કરો. | `LABELS.errorFallback`         | Transient retrieval failure message            |

---

## 5. Verification & Testing Standards

- **Glyph Rendering:** Unit test suite in `gujarati.test.ts` validates numeral conversion across digits, dates, and edge cases.
- **Accessibility:** Headings and truncated elements provide descriptive accessible titles and labels without silent information loss.
