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
