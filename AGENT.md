# Working Agreement

This agreement applies to every developer, contributor, and AI coding agent working in this repository.

## 1. Before Work

1. Read this agreement, the root [README](README.md), [PRD](docs/product/PRD.md), [Architecture](docs/architecture/ARCHITECTURE.md), [Decisions](docs/architecture/DECISIONS.md), and [Code Rules](docs/engineering/CODE_RULES.md).
2. For multi-tenant migration work, consult the relevant sprint prompt and [Multi-Tenant Rollout Plan](docs/operations/TENANT_ROLLOUT.md).
3. Check the current branch and `git status`. Never work directly on `main` or push to production.
4. Review recent commits and test suite status before modifying code.

## 2. Product vs. Deployment Scope

- **Product:** **Rorfost School Platform** — a reusable, multi-tenant modular monolith for primary and secondary schools.
- **Reference Deployment (As-Is):** **Dhadhana portal** (Dhadhana Primary School) — the first production instance.
- Distinguish generic software architecture from school-specific content/branding.
- Do not assume multi-tenancy is active in current code; respect the documented **as-is** vs. **target** state.

## 3. Strict Scope & Incremental Sprints

Work strictly within the requested scope. Do not silently rewrite architecture, introduce speculative abstractions, or touch unrelated packages. When executing migration sprints, complete only the single designated micro-sprint and its acceptance tests.

## 4. Excel & Student Privacy Rule

**Never invent or assume unverified student result schemas.** Student data privacy is paramount:
- Government identifiers (such as Aadhaar UID) must NEVER be read, persisted, logged, or hashed.
- Student results must use school-controlled roll numbers and secure result access policies.
- See [RESULT_IMPORT.md](docs/data/RESULT_IMPORT.md) and [EXAM_RESULT_SYSTEM_FLOW.md](docs/EXAM_RESULT_SYSTEM_FLOW.md).

## 5. Gujarati UI and Design Standards

All student-, parent-, and visitor-facing interfaces must use clear, natural, simple Gujarati copy. English is reserved for developer code, APIs, database tables, and system logs.
- Mobile-first, calm, high-contrast, professional, and accessible.
- Avoid unnecessary animations, glassmorphism, heavy gradients, or generic SaaS marketing copy.

## 6. Code Quality & Security by Default

- Keep methods and components small, cohesive, and feature-scoped. Follow [CODE_RULES.md](docs/engineering/CODE_RULES.md).
- Never trust client-supplied tenant identifiers (`schoolId`, `slug`, `Host`, or custom headers). In target architecture, all tenant resolution must derive from verified gateway credentials or server-side sessions.
- Comments must explain **why** (constraints, security rationale, business rules), not narrate self-explanatory code. Never add automated-tool attribution in code or commits.

## 7. Commits and Verification

- Run relevant frontend and backend checks prior to committing.
- Commit messages must be clear, concise, and professional.
- Never commit credentials, production secrets, real student workbooks, or database dumps.
