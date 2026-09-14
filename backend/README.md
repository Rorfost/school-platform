# Backend

This directory is reserved for the future Java 21 Spring Boot 4.x Maven modular monolith. Domain-feature implementation has intentionally not begun.

The planned backend exposes REST endpoints, uses PostgreSQL with Flyway, Spring Security with Spring Session JDBC, and an S3-compatible storage abstraction (MinIO locally, Cloudflare R2 in production). Controllers return DTOs, never JPA entities.

See the root [README](../README.md), [architecture](../docs/architecture/ARCHITECTURE.md), [security](../docs/engineering/SECURITY.md), and [configuration](../docs/engineering/CONFIG.md).
