# Product Requirements Document

## Purpose and problem

Rorfost School Portal gives a small school a clear public presence and a focused way for its principal to publish school information and student resources. It replaces ad hoc sharing of notices, files, galleries, and results with a simple, privacy-conscious website. It must be portable: branding, school information, and future school-specific configuration must be changeable by an administrator rather than by editing source code.

## Users and primary use cases

- **Principal/admin:** configures the school, maintains public content and academic settings, uploads files, imports results, and explicitly publishes results.
- **Students (Standard 1–8):** use a phone-first Gujarati interface to find study materials, timetables, downloads, notices, and their individual results.
- **Parents, teachers, and visitors:** view public school information, notices, gallery, contact details, and relevant resources.

## Main modules

Public home, About School, Principal's Desk, notices, Student Corner (study materials, results, Ekam Kasoti, timetables, downloads), gallery, and contact. The future principal area manages school configuration, branding, principal profile, academic years, standards, subjects, assessments, results, study material, notices, gallery, downloads, and timetables.

Assessments are one generic domain with types such as `EKAM_KASOTI`, `UNIT_TEST`, `PERIODIC_TEST`, `SEMESTER`, `ANNUAL`, and `OTHER`. Imported results begin as draft and require explicit publication.

## V1 scope

V1 is a small, single-principal portal with public content and individual public result lookup. It uses a React frontend, Spring Boot modular monolith, PostgreSQL, and ImageKit file storage. Students do not authenticate. Public result lookup will eventually use academic year, standard, assessment, roll number, and a result PIN; details remain subject to the supplied result workflow.

## Excluded from V1

Unless requirements change, V1 excludes student, parent, and teacher accounts; attendance ERP; fee payments; online exams; chat or messaging; payroll; transport tracking; biometrics; and full ERP features. It also excludes Redis, queues, microservices, GraphQL, and other infrastructure without a demonstrated need.

## Experience requirements

All user-facing UI—including admin, errors, labels, validation, empty states, and dialogs—must be Gujarati-first, using clear natural language appropriate for children, parents, and a principal. Public experiences are mobile-first, accessible, calm, professional, high-contrast, and touch-friendly. Do not make the portal flashy, heavily animated, overly colorful, or SaaS-like.

## Success criteria

- A principal can change school-owned configuration without source edits.
- Visitors can find public information and resources easily on a phone.
- Result data remains private, individually accessed, non-cacheable, and unpublished until the principal acts.
- The system remains understandable and inexpensive for roughly 100–500 users.
- Another developer can safely extend it by following repository documentation.

## Privacy principles

Collect only data required for the portal. Do not add government IDs, home addresses, dates of birth, parent private information, or phone numbers without a real approved requirement. Store files outside the database; store result PINs only as secure hashes. Never expose a whole class's results publicly.
