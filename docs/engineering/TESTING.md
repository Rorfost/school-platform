# Testing Strategy

Backend testing will use JUnit 5, Mockito, Spring Boot Test, MockMvc, and Testcontainers PostgreSQL. Frontend testing will use Vitest and React Testing Library. Playwright will cover critical end-to-end journeys.

Test behavior at the appropriate level: service rules and validation, controller authorization/error contracts, repository/migration integration, accessible Gujarati UI states, and mobile-responsive critical flows. Include negative security tests for admin access, CSRF, result lookup enumeration, and upload validation.

The planned end-to-end result journey is principal login → create assessment → import valid workbook → preview → confirm → publish → student lookup. The Excel-dependent portion is deferred until the owner provides the actual workbook contract; no fixture or parser schema should be invented before then.
