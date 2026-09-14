# Local Development

The intended local topology is Vite at `localhost:5173`, Spring Boot at `localhost:8080`, PostgreSQL at `localhost:5432`, and MinIO at `localhost:9000` (console `localhost:9001`). Docker Compose runs PostgreSQL and MinIO; frontend and backend will run natively after their scaffolds are added.

1. Install Node 24 LTS or newer and Java 21 (plus Maven). Copy the component `.env.example` files to untracked local environment files only when your shell tooling needs them.
2. Start infrastructure with `docker compose up -d`.
3. In `frontend`, run `npm install` then `npm run dev`. In `backend`, run `mvn spring-boot:run -Dspring-boot.run.profiles=local`.
4. Stop infrastructure with `docker compose down`; use `docker compose down -v` only when intentionally discarding local database/object data.

The backend applies its Flyway-owned Spring Session JDBC migration on first local startup. The default Compose credentials are local-only placeholders. Do not reuse them outside local development. Production configuration must be supplied through protected host settings.
