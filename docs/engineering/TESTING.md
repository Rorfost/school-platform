# Testing Strategy — Rorfost School Platform

## Test Hierarchy & Execution

Quality and multi-tenant security are validated through layered testing:

```
┌────────────────────────────────────────────────────────┐
│  Cross-Tenant & Negative Security Tests (Sprint 24)    │
│  - Spoofed Gateway Tokens, Cross-School Session Replay │
├────────────────────────────────────────────────────────┤
│  Persistence & Migration Tests (Testcontainers)        │
│  - Real PostgreSQL, Append-Only Flyway V1-V24+         │
├────────────────────────────────────────────────────────┤
│  Backend Service & Controller Unit Tests (JUnit 5)     │
│  - Validation, Authorization, Business Rules, Mocks    │
├────────────────────────────────────────────────────────┤
│  Frontend Test Suite (Vitest & RTL)                    │
│  - UI Render, Gujarati Copy, Responsive State, A4 Print│
└────────────────────────────────────────────────────────┘
```

## Backend Testing

- **Unit Testing:** JUnit 5, Mockito, Spring Boot Test, and MockMvc verify domain rules, controller status codes, problem details, and service transactions. Run with:
  ```bash
  mvn test
  ```
- **Persistence & Integration:** Uses Testcontainers with real PostgreSQL to execute all Flyway migrations and verify relational integrity and composite foreign keys.
  > **Note on Local Execution:** If the local Docker daemon is not active, run unit tests via `mvn test -Dtest="!*IntegrationTest"`. Integration tests are mandatory and enforced in CI prior to release.
- **Storage Testing:** ImageKit operations are tested using isolated mock doubles and fake storage providers; tests never connect to live ImageKit accounts.

## Frontend Testing

- **Unit & Component Testing:** Vitest and React Testing Library verify component behavior, modal interactions, form validations, and accessible roles.
- **Linting & Typechecking:** Prettier format checks, ESLint rules, and TypeScript typechecking are enforced:
  ```bash
  npm run format:check && npm run lint && npm run typecheck && npm run test && npm run build
  ```

## Multi-Tenant Negative Security Tests

In accordance with the multi-tenant rollout plan, cross-tenant negative tests verify that:
1. Requests lacking `X-Portal-Gateway-Token` are rejected.
2. Forged headers (`X-School-Id`, `X-Forwarded-Host`) cannot override tenant resolution.
3. Principal credentials for School A cannot authenticate on School B's portal.
4. An active session for School A replayed on School B's portal is denied with 403.
5. School A cannot view, update, upload, or delete data belonging to School B.

## Test Data Discipline

All test fixtures must use synthetic, fictitious student names, sample marks, and non-sensitive identifiers. Real student names, G.R. numbers, Aadhaar numbers, or production database dumps are strictly prohibited from test suites.
