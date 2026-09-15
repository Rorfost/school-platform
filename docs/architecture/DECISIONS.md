# Architecture Decisions

Each decision is intentional and may be revisited only when a concrete requirement changes.

## Frontend: React + Vite, not Next.js

**Context:** A small public/admin portal needs a simple static frontend and REST API. **Decision:** React, TypeScript, and Vite. **Reason:** fast, conventional, low-cost deployment to Cloudflare. **Trade-offs:** server rendering is not available by default. **Reconsider when:** SEO or server-rendered personalization becomes a demonstrated need.

## Backend: Spring Boot modular monolith

**Context:** One small domain and one initial admin. **Decision:** Java 21 Spring Boot 4.x with feature-based packages. **Reason:** strong security/data support without distributed operations. **Trade-offs:** one deployable unit. **Reconsider when:** independently scaling or owning a domain has proven value.

## PostgreSQL and Flyway, not MongoDB

**Context:** Academic data, configuration, and results are relational and auditable. **Decision:** PostgreSQL with every schema change through Flyway. **Reason:** transactions, relationships, constraints, and reliable migrations. **Trade-offs:** schema discipline is required. **Reconsider when:** a documented data requirement no longer fits relational storage.

## R2-compatible object storage

**Context:** Images and documents should not be database blobs. **Decision:** Cloudflare R2 in production, MinIO locally, via AWS SDK v2 S3 abstraction. **Reason:** portable object keys and inexpensive file delivery. **Trade-offs:** storage coordination and cleanup are required. **Reconsider when:** file access or compliance needs change.

## Spring Session JDBC; no Redis initially

**Context:** Admin sessions require server-side control and one backend instance is expected. **Decision:** Spring Session JDBC in PostgreSQL. **Reason:** fewer services, durable sessions, simple operations. **Trade-offs:** database session traffic. **Reconsider when:** multiple instances or measured load requires a dedicated shared cache/session store.

## No student authentication; generic assessments

**Context:** Students should access only individual results without account administration. **Decision:** no student/parent/teacher accounts; one Assessment model with types. **Reason:** privacy and lower operational burden, while avoiding separate result systems. **Trade-offs:** lookup security needs careful design. **Reconsider when:** approved account or workflow requirements exist.

## Gujarati-first and mobile-first

**Context:** children, parents, and principal will frequently use phones. **Decision:** Gujarati-first visible UI and mobile-first design. **Reason:** clarity and practical access. **Trade-offs:** copy review and responsive testing are mandatory. **Reconsider when:** explicit multilingual requirements are approved.

## Production direction and local parity

**Context:** keep initial hosting inexpensive and portable. **Decision:** Cloudflare frontend/DNS/R2, Render Free backend initially, Aiven PostgreSQL; MinIO locally. **Reason:** managed services with no provider-specific application design. **Trade-offs:** Render Free availability limitations. **Reconsider when:** reliability, cost, or region requirements change.

## V1 database foundation before feature workflows

**Context:** later public/admin features need reliable history, privacy, and shared constraints. **Decision:** establish the complete V1 relational foundation through Flyway before controllers are delivered. **Reason:** migrations, composite school scope, and PostgreSQL constraints become the single stable contract instead of being re-designed feature by feature. **Trade-offs:** the schema includes deliberately inactive persistence boundaries before their services exist. **Reconsider when:** approved product requirements materially change a documented V1 field or lifecycle.

## Generic assessments and deferred workbook behavior

**Context:** the portal supports several assessment labels but the owner has not supplied the Excel workbook. **Decision:** seed generic assessment types and store only one numeric subject score model; do not create import tables, parser logic, totals, grades, absence fields, or workbook mappings. **Reason:** generic assessment structure is documented, while workbook-specific behavior is not. **Trade-offs:** future result-import work needs a new migration after workbook review. **Reconsider when:** the approved workbook contract defines required data.

## V1 operational metadata and assessment publication

**Context:** school configuration, content management, and generic assessment APIs need fields beyond the initial persistence boundary. **Decision:** add forward-only Flyway migration V6 for optional school and principal contact/profile fields, ordered standard-subject mappings, configured per-subject marks, notice pin/expiry, gallery covers, and material type. An assessment can publish only after its subject configuration is complete; result-data publication remains a separate future import/release decision. **Reason:** the first condition is known now, while the real workbook has not established result-data rules. **Trade-offs:** a later result-import migration may add a stricter publication gate. **Reconsider when:** the approved workbook contract defines it.

## Storage upload policy and consistency

**Context:** V1 needs public documents and images without treating object storage as a database. **Decision:** accept only signature-checked PDF, JPEG, PNG, and WebP uploads, generate UUID object keys, and retain original filename plus checksum in metadata. Upload callers compensate by deleting a newly stored object when metadata persistence fails; deletion removes storage before metadata so a storage failure retains the database reference for retry. **Reason:** it avoids executable uploads and obvious silent orphaning without a speculative cleanup subsystem. **Trade-offs:** a rare double failure requires operational reconciliation of bucket prefixes against metadata. **Reconsider when:** asynchronous cleanup or private upload workflows are approved.

## Standard 3 workbook identity boundary

**Context:** the observed Tri-masik Ekam Kasoti Standard 3 workbook has subject marks and totals but uses Aadhaar UID as its only row identity, with no roll number or result PIN. **Decision:** document the exact workbook layout but do not parse, persist, hash, or otherwise retain Aadhaar values; do not invent roll numbers, PINs, or a public lookup workflow. **Reason:** government identifiers are outside the portal’s privacy model and the existing student identity requires a school-controlled roll number. **Trade-offs:** this format cannot enter the transactional import lifecycle until the owner supplies a privacy-safe mapping or workbook revision. **Reconsider when:** a non-government school identifier and PIN distribution decision are approved.

## PostgreSQL integrity for school scope and lifecycle

**Context:** one school runs initially, but reuse must be safe without an expensive redesign. **Decision:** retain `school_id` on important records, use composite foreign keys where relationships cross school-scoped tables, enforce publication/archive states and one current academic year in PostgreSQL. **Reason:** application code alone cannot reliably protect cross-school references or concurrent lifecycle changes. **Trade-offs:** selected tables carry integrity-key repetition and migrations are more detailed. **Reconsider when:** a confirmed multi-school authorization model needs a different ownership boundary.

## No infrastructure directory at this stage

**Context:** the inspected `infrastructure/` directory contained guidance only; Docker Compose already has the conventional root location and no provider/runtime asset is versioned yet. **Decision:** remove the empty infrastructure boundary rather than retain a placeholder directory. **Reason:** the repository should not imply deployment assets exist when there are none. **Trade-offs:** a future genuine provider-neutral script or deployment asset will reintroduce the directory with that asset and documentation. **Reconsider when:** such an asset is required.

## Testcontainers 2.x for current Docker engines

**Context:** the existing Testcontainers 1.21.3 client cannot negotiate with the installed Docker Engine 29 API, blocking PostgreSQL persistence tests. **Decision:** use the current Testcontainers 2.x test-only modules. **Reason:** it supports current Docker engine behavior while retaining disposable PostgreSQL integration tests. **Trade-offs:** module artifact names use the 2.x `testcontainers-` prefix. **Reconsider when:** the project standardizes on a managed dependency version that provides the same compatibility.

## Principal session authentication in PostgreSQL

**Context:** the browser-based admin panel needs revocable server-side authentication while the first deployment remains a single instance with a limited operational footprint. **Decision:** use Spring Security, Spring Session JDBC, PostgreSQL, BCrypt, and the sole `PRINCIPAL` role. **Reason:** this provides secure session invalidation, durable sessions, and adaptive password hashing without JWT or Redis. **Trade-offs:** each authenticated request reads session state from PostgreSQL. **Reconsider when:** measured multi-instance scale needs a dedicated session store.

## Explicit CSRF and bootstrap boundaries

**Context:** cookie authentication needs browser request protection, and `admin_users` correctly requires an existing school. **Decision:** keep CSRF enabled through a token endpoint and require email, password, and school slug for first-principal bootstrap. Bootstrap creates only when no administrator exists and never updates existing credentials. **Reason:** the SPA has an explicit CSRF contract and the bootstrap cannot silently create speculative school data. **Trade-offs:** initial deployment must create the school record before enabling bootstrap. **Reconsider when:** a documented school-provisioning workflow is implemented.

## Local login rate limiter

**Context:** repeated authentication attempts need a bounded control before any distributed infrastructure is justified. **Decision:** use a configurable in-memory rolling-window limiter keyed by direct client address. **Reason:** it protects the only current authentication endpoint without adding Redis or another service. **Trade-offs:** limits reset on restart and do not coordinate across instances. **Reconsider when:** deployment topology has more than one backend instance or measured abuse requires a shared control.
