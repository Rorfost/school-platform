# Local Development

The intended local topology is Vite at `localhost:5173`, Spring Boot at `localhost:8080`, PostgreSQL at `localhost:5432`, and MinIO at `localhost:9000` (console `localhost:9001`). Docker Compose runs PostgreSQL and MinIO; frontend and backend will run natively after their scaffolds are added.

1. Copy the component `.env.example` files to untracked `.env` files when implementation starts.
2. Start infrastructure with `docker compose up -d`.
3. Bootstrap the frontend and backend only as part of the roadmap's Phase 0; follow their component READMEs.
4. Stop infrastructure with `docker compose down`; use `docker compose down -v` only when intentionally discarding local database/object data.

The default Compose credentials are local-only placeholders. Do not reuse them outside local development. Production configuration must be supplied through protected host settings.
