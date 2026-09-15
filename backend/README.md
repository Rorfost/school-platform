# Backend

The backend is a Java 21 Spring Boot 4.x Maven modular monolith. It includes Flyway-owned PostgreSQL schema, JDBC-backed principal sessions, BCrypt authentication, CSRF/CORS/security-header controls, safe audit events, request IDs, restricted Actuator health, and an S3-compatible object-storage abstraction. It intentionally contains no result parsing or Excel assumptions.

## Commands

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=local
mvn test
mvn verify
mvn spotless:apply
```

Use Java 21 for development and deployment. Java versions newer than 21 can compile with the configured release target, but Java 21 is the supported baseline. Copy `.env.example` to an untracked local environment file only if your shell tooling loads it; Spring itself reads environment variables.

## Configuration

Profiles are `local`, `test`, and `prod`. `application.yml` contains shared safe defaults, while profile files select environment-specific behavior. Secrets are always environment variables. The local profile connects to Docker Compose PostgreSQL and MinIO; production remains portable to Render or another standard JVM/container host, Aiven PostgreSQL, and Cloudflare R2.

## Structure

- `common/config` — properties and shared infrastructure configuration
- `common/security` — security, CORS, CSRF, and session-compatible defaults
- `common/storage` — provider-neutral object storage interface and S3 implementation
- `common/exception` — API error and request identifier foundation

The `auth` package owns principal login, bootstrap, password change, rate limiting, and DTOs. The `school`, `academic`, `assessment`, `content`, and `audit` packages retain feature-oriented persistence boundaries. See the root [README](../README.md) and [engineering documentation](../docs/engineering/SECURITY.md).

## Current backend scope

Authentication, school/profile, academic configuration, generic assessment configuration, and public/admin content API foundations are implemented. Result import and individual result lookup are blocked until the owner provides a privacy-safe school roll-number and PIN workflow for the observed Standard 3 workbook. Other exam formats and timetable features remain pending source material.
