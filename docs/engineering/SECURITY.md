# Security and Privacy

Spring Security protects admin endpoints. The initial role is `PRINCIPAL`; passwords use BCrypt and sessions use Spring Session JDBC. Cookies must be HttpOnly, Secure in production, and use an appropriate SameSite policy. State-changing browser requests require CSRF protection. CORS uses explicit configured origins, never a permissive wildcard with credentials.

Result lookups must resist enumeration: do not expose class-wide results, hash result PINs, return generic invalid-result responses, rate-limit attempts with a lightweight in-memory policy when implemented, and send `Cache-Control: no-store` for successful and unsuccessful private-result responses.

Validate uploads on the server for authorization, size, declared type, detected content where feasible, and allowed MIME/extension policy. Store private files privately and use controlled access paths. Do not trust client filenames or content type. Define exact limits before upload features are released.

Send appropriate security headers, avoid sensitive logs, never commit secrets, and do not expose sensitive Actuator endpoints publicly. Audit principal actions and sensitive bulk operations. Apply least-data collection, protect backups, and restrict database/storage credentials to the backend.
