# Testing Strategy

Backend testing uses JUnit 5, Mockito, Spring Boot Test, MockMvc, and PostgreSQL Testcontainers. The persistence integration suite starts PostgreSQL, applies every Flyway migration to an empty database, validates Hibernate mappings, and verifies important database constraints. It never substitutes H2 for PostgreSQL. The foundation also includes a focused S3-compatible storage delegation test. Frontend testing uses Vitest and React Testing Library, with a Gujarati home-shell rendering test. Playwright will cover critical end-to-end journeys.

Test behavior at the appropriate level: service rules and validation, controller authorization/error contracts, repository/migration integration, accessible Gujarati UI states, and mobile-responsive critical flows. Include negative security tests for admin access, CSRF, result lookup enumeration, and upload validation.

The planned end-to-end result journey is principal login → create assessment → import valid workbook → preview → confirm → publish → student lookup. The Excel-dependent portion is deferred until the owner provides the actual workbook contract; no fixture or parser schema should be invented before then.
