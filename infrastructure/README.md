# Infrastructure

This directory is the home for versioned infrastructure assets that genuinely need to grow beyond the local stack: portable deployment manifests, validated helper scripts, or provider-neutral operational configuration.

The active local entry point intentionally remains the root [docker-compose.yml](../docker-compose.yml). Docker Compose discovers that conventional file automatically, and keeping PostgreSQL and MinIO there preserves the documented `docker compose up -d` workflow. The current stack is deliberately limited to PostgreSQL and MinIO; it does not run application containers, Redis, or production provider configuration.

Add assets here only when there is a real infrastructure need. Keep Cloudflare, Render, Aiven, and R2 credentials/configuration in their protected provider settings, not in this repository.
