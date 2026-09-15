# Backend

The backend is a Java 21 Spring Boot 4.x Maven modular monolith foundation. It includes configuration, secure defaults, Flyway-owned Spring Session JDBC and V1 domain schema, feature-oriented JPA persistence boundaries, an S3-compatible object-storage abstraction, request IDs, `ProblemDetail` exception handling, and a restricted Actuator health endpoint. It intentionally contains no controllers, login flow, result parsing, or Excel assumptions.

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

The `school`, `academic`, `assessment`, `content`, and `audit` packages provide V1 persistence models and repositories only. Services and HTTP adapters remain feature-phase work. See the root [README](../README.md) and [engineering documentation](../docs/engineering/CODE_RULES.md).
