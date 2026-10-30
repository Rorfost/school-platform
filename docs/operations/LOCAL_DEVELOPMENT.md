# Local Development — Rorfost School Platform

## Local Topology

The local development environment uses:
- **Frontend:** Vite dev server at `http://localhost:5173` (proxies `/api` to port 8080 or calls it via CORS).
- **Backend:** Spring Boot application at `http://localhost:8080` (profile: `local`).
- **Database:** PostgreSQL container at `localhost:5432` via Docker Compose.
- **Storage:** ImageKit integration using test environment keys.

## Quick Start

1. **Prerequisites:** Node.js 24+, Java 21, Maven 3.9+, Docker Desktop.
2. **Start Database:**
   ```bash
   docker compose up -d
   ```
3. **Start Frontend:**
   ```bash
   cd frontend
   npm install
   npm run dev
   ```
4. **Start Backend:**
   ```bash
   cd backend
   mvn spring-boot:run -Dspring-boot.run.profiles=local
   ```
5. **Stop Database:**
   ```bash
   docker compose down
   # To discard all local database data:
   docker compose down -v
   ```

## Multi-Tenant Local Testing Notes

- **Current State (As-Is):** First startup applies Flyway migrations V1–V24 and uses placeholder school data.
- **Target Gateway Testing (Sprints 03–05):** In `local` profile, the gateway filter permits a configured local test token or defaults to an active local school to simplify frontend development without requiring Wrangler CLI locally.
- **Spotless & Linting:**
  ```bash
  # In backend:
  mvn spotless:check
  mvn spotless:apply

  # In frontend:
  npm run format:check
  npm run lint
  ```
