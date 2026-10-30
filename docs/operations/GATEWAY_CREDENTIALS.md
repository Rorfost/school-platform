# Gateway Credentials & Secret Rotation Policy

## 1. Overview

The Rorfost School Platform uses an edge-authenticated reverse-proxy routing model. Each school portal is served by an independent Cloudflare Worker edge deployment that proxies `/api/*` requests to the shared Spring Boot backend on Render.

To securely resolve the target school without relying on spoofable HTTP headers (`Host`, `Origin`, `Referer`, `X-Forwarded-*`), each Cloudflare Worker holds a private `PORTAL_GATEWAY_TOKEN` secret. For all `/api/*` calls, the Worker injects this token into the `X-Portal-Gateway-Token` request header.

The backend verifies the incoming token by hashing it with SHA-256 and matching it against active records in the `school_gateway_credentials` table.

---

## 2. Cryptographic Token Specification

- **Token Format:** Opaque random string containing at least 256 bits (32 bytes) of cryptographic entropy, Base64- or Hex-encoded (e.g., `openssl rand -hex 32` or `crypto.getRandomValues(new Uint8Array(32))`).
- **Storage Policy:**
  - **Cloudflare Worker:** Stored securely as an encrypted Worker secret (`wrangler secret put PORTAL_GATEWAY_TOKEN --env <school_env>`). Never committed to repository files (`wrangler.jsonc`), source code, or frontend client bundles.
  - **Database (Neon/PostgreSQL):** Stored strictly as a 32-byte `BYTEA` SHA-256 hash in `school_gateway_credentials.token_digest`. **Plaintext tokens are never persisted or logged**.
  - **Backend Memory:** The incoming header value is held only for the duration of the SHA-256 digest calculation in `PortalGatewayFilter`.

---

## 3. Database Schema

Managed by Flyway migration `V25__create_school_gateway_credentials.sql`:

```sql
CREATE TABLE school_gateway_credentials (
    id UUID PRIMARY KEY,
    school_id UUID NOT NULL,
    token_digest BYTEA NOT NULL,
    label VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    rotated_at TIMESTAMPTZ,
    CONSTRAINT school_gateway_credentials_school_fk
        FOREIGN KEY (school_id) REFERENCES schools (id) ON DELETE RESTRICT,
    CONSTRAINT school_gateway_credentials_label_not_blank
        CHECK (btrim(label) <> ''),
    CONSTRAINT school_gateway_credentials_expiry_valid
        CHECK (expires_at IS NULL OR expires_at > created_at)
);

CREATE UNIQUE INDEX school_gateway_credentials_digest_unique
    ON school_gateway_credentials (token_digest);

CREATE INDEX school_gateway_credentials_school_id_idx
    ON school_gateway_credentials (school_id);

CREATE INDEX school_gateway_credentials_active_idx
    ON school_gateway_credentials (school_id, created_at DESC)
    WHERE revoked_at IS NULL;
```

---

## 4. Credential States & Validation

A credential is valid and active if and only if:
1. `revoked_at IS NULL`
2. `expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP`
3. Associated `schools.is_active = TRUE`

If any of these conditions fail, gateway verification fails closed with HTTP 403 Forbidden without leaking school existence or diagnostic details.

---

## 5. Zero-Downtime Secret Rotation Procedure

To support scheduled secret rotation or incident response without interrupting school operations:

### Rule: At Most Two Active Credentials Per School
During rotation, a school will briefly have two valid credentials (the retiring credential and the incoming replacement). Once the Worker secret is updated and verified, the retiring credential is immediately revoked.

### Step-by-Step Procedure:

1. **Generate New Secret:**
   Operator generates a fresh random token offline:
   ```bash
   NEW_TOKEN=$(openssl rand -hex 32)
   NEW_DIGEST=$(echo -n "$NEW_TOKEN" | openssl dgst -sha256 -binary | xxd -p -c 32)
   ```

2. **Register Replacement Credential in Database:**
   Insert the new token digest for the target school with label `e.g. Worker-SchoolA-2026-Q4`:
   ```sql
   INSERT INTO school_gateway_credentials (
       id, school_id, token_digest, label, created_at
   ) VALUES (
       gen_random_uuid(),
       '<SCHOOL_UUID>',
       decode('<NEW_DIGEST_HEX>', 'hex'),
       'Worker-SchoolA-2026-Q4',
       CURRENT_TIMESTAMP
   );
   ```

3. **Update Cloudflare Worker Secret:**
   Deploy the new secret to the specific school environment:
   ```bash
   echo "$NEW_TOKEN" | npx wrangler secret put PORTAL_GATEWAY_TOKEN --env school_a
   ```

4. **Verify Routing:**
   Perform health and API verification on `https://school-a.rorfost.workers.dev/api/v1/public/school`. Verify the `last_used_at` timestamp updates on the new credential.

5. **Revoke Retiring Credential:**
   Mark the previous credential as revoked and record rotation metadata:
   ```sql
   UPDATE school_gateway_credentials
   SET revoked_at = CURRENT_TIMESTAMP,
       rotated_at = CURRENT_TIMESTAMP
   WHERE school_id = '<SCHOOL_UUID>'
     AND label = '<OLD_CREDENTIAL_LABEL>'
     AND revoked_at IS NULL;
   ```

6. **Verify Single Active Credential:**
   Ensure `countBySchoolIdAndRevokedAtIsNull(school_id) == 1`.

---

## 6. Access Control & Operator Boundary

- Gateway credential management is strictly an **infrastructure operator** task.
- Principals, teachers, and school administrators have **zero** access to gateway credentials or management endpoints.
- Credential operations are executed via direct operator database tools or future platform administrative scripts; no public REST API exists for inserting or viewing credentials.
