# Provider Free-Tier Quotas & Operational Capacity Runbook (Sprint 58)

**Document Version:** 1.0  
**Repository:** https://github.com/Rorfost/school-platform  
**Scope:** Inventory of free-tier thresholds across infrastructure providers, manual dashboard inspection procedures, no-cost alert configurations, and escalation procedures without paid subscriptions.

---

## 1. Zero-Cost Infrastructure Principles

1. **Strictly Free-Tier:** All deployment configurations must fit entirely within provider free tiers (Render Free, Cloudflare Free, Neon Free, ImageKit Free).
2. **No Automated Billing Upgrades:** Never configure auto-upgrade, pay-as-you-go elastic thresholds, or automated credit card billing triggers. If a quota is exceeded, the service must fail gracefully with natural localized user guidance rather than incurring unplanned debt.
3. **Multi-Tenant Protection:** A quota breach on one school (e.g. excessive image uploads) must not corrupt or leak another tenant's data.

---

## 2. Provider Free-Tier Limits & Behavior Under Exhaustion

| Provider       | Component                     | Free Tier Monthly / Account Limit                                                 | Behavior When Exhausted                                                                    | Client / System Mitigation                                                                                                                                              |
| :------------- | :---------------------------- | :-------------------------------------------------------------------------------- | :----------------------------------------------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Render**     | Web Service (Spring Boot API) | 750 free instance hours / month. Spins down after 15 min idle.                    | Service suspended if instance hours expire for the month; cold start takes 40–50s on wake. | Client applies 45s timeout with Gujarati loading/retry indicators. No writes are replayed automatically.                                                                |
| **Neon**       | PostgreSQL Database           | 0.5 GiB storage, 100 compute-hours / month, max 10 concurrent pooled connections. | Database suspends when compute quota is exhausted; connection refusals return 503.         | HikariCP pool sized conservatively (`DB_POOL_MAX_SIZE=3`). Client reports clean Gujarati service unavailable message.                                                   |
| **Cloudflare** | Workers & Edge CDN            | 100,000 requests / day across all Workers on the account.                         | Worker returns HTTP 1010 or 503 when daily limit is reached.                               | Static assets served via assets binding; public API GET responses cached at edge with TTLs to minimize worker compute invocations.                                      |
| **ImageKit**   | Media Storage & CDN           | 20 GB bandwidth / month, 20 GB media storage.                                     | ImageKit returns HTTP 429 (Too Many Requests) or falls back to error images.               | Responsive media transformations bounded (`tr=w-400`, `tr=w-800`), avatar thumbs cached. Client maps 429 to clear Gujarati rate-limit guidance without retrying writes. |

---

## 3. Manual Provider Dashboards & No-Cost Monitoring

Operators monitor platform capacity manually using free native provider dashboards. **No paid third-party APM or monitoring SaaS is required or permitted.**

### 3.1 Render Dashboard

- **URL:** `https://dashboard.render.com`
- **Inspection Metrics:** Active instance status, outbound bandwidth, memory utilization (< 512 MB).
- **Free Alerts:** Email notifications on service failure or monthly free hour consumption threshold (75%).

### 3.2 Neon Console

- **URL:** `https://console.neon.tech`
- **Inspection Metrics:** Storage consumption, compute hours used, active connection pool gauge.
- **Action on High Storage:** Archive old exam result workbooks or delete obsolete soft-deleted records using authorized manual operator scripts.

### 3.3 Cloudflare Dashboard

- **URL:** `https://dash.cloudflare.com`
- **Inspection Metrics:** Daily Worker requests, Edge Cache hit ratio, Subrequest count.
- **Free Notifications:** Configure Cloudflare Notification Center email alerts for "Worker Daily Request Limit Exceeded".

### 3.4 ImageKit Dashboard

- **URL:** `https://imagekit.io/dashboard`
- **Inspection Metrics:** Bandwidth usage, storage usage, top transformed assets.
- **Action on High Bandwidth:** Inspect caching headers and ensure client components request appropriate WebP/responsive dimensions.

---

## 4. Cold Start & Failure Resilience Matrix

| Scenario                            | HTTP Status             | Client Retry Policy                                                                   | User Experience (Gujarati)                                                                               |
| :---------------------------------- | :---------------------- | :------------------------------------------------------------------------------------ | :------------------------------------------------------------------------------------------------------- |
| **Render Spin-Up (Cold Start)**     | Pending / 408 Timeout   | Reads: max 1 retry after initial attempt. Writes: **0 retries** (manual button only). | _"સર્વર પ્રતિસાદમાં વધુ સમય લાગી રહ્યો છે (સર્વર શરૂ થઈ રહ્યું હોઈ શકે છે). કૃપા કરીને ફરી પ્રયાસ કરો."_ |
| **Neon Connection Pool Maxed**      | 503 Backend Unavailable | 0 auto-retries for mutating requests.                                                 | _"સર્વર હાલ અનુપલબ્ધ છે. કૃપા કરીને થોડીવાર પછી ફરી પ્રયાસ કરો."_                                        |
| **ImageKit Quota / 429 Rate Limit** | 429 Too Many Requests   | 0 auto-retries. Prevents cascading quota drain.                                       | _"વિનંતી મર્યાદા ઓળંગી ગઈ છે. કૃપા કરીને થોડીવાર પછી ફરી પ્રયાસ કરો."_                                   |
| **Cloudflare Daily Worker Limit**   | 503 Service Unavailable | 0 auto-retries.                                                                       | _"સેવા હાલ અનુપલબ્ધ છે. કૃપા કરીને થોડીવાર પછી ફરી પ્રયાસ કરો."_                                         |

---

## 5. Escalation & Recovery Contacts

- **Primary Platform Operator:** Registered system administrator email on Render / Cloudflare account.
- **Incident Escalation Path:**
    1. Verify outage type via Cloudflare / Render status pages.
    2. If Render free hours exhausted, service resumes on 1st of next calendar month unless manual off-cycle restart or secondary free workspace is designated.
    3. Never enter credit card or activate automated billing without explicit stakeholder authorization.
