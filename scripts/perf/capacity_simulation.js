/**
 * Multi-Tenant Capacity & Synthetic Load Simulator (Sprint 59)
 *
 * Simulates bounded concurrent read-heavy and results-day traffic profiles
 * across 3, 10, and 100 modeled tenant records without hitting public infrastructure.
 *
 * Usage:
 *   node scripts/perf/capacity_simulation.js [--schools=3|10|100] [--concurrency=N] [--samples=N]
 */

import http from "http";
import https from "https";

const args = process.argv.slice(2);
function getArg(prefix, fallback) {
    const match = args.find((a) => a.startsWith(prefix));
    return match ? match.split("=")[1] : fallback;
}

const SCHOOL_COUNT = parseInt(getArg("--schools", "3"), 10);
const CONCURRENCY = parseInt(getArg("--concurrency", "5"), 10);
const SAMPLES = parseInt(getArg("--samples", "10"), 10);
const TARGET_HOST = process.env.BENCHMARK_HOST || "http://localhost:8080";

// Generate synthetic school profiles
function generateSyntheticSchools(count) {
    const schools = [];
    for (let i = 1; i <= count; i++) {
        schools.push({
            id: `synth-school-${i}`,
            name: `Synthetic School ${i}`,
            slug: `school-${i}`,
            token: `worker-secret-school-${i}`,
        });
    }
    return schools;
}

const SCHOOLS = generateSyntheticSchools(SCHOOL_COUNT);

// Read-heavy public routes and results-day lookup
const SCENARIOS = [
    { path: "/api/v1/public/school", weight: 4, desc: "School Info" },
    { path: "/api/v1/public/notices", weight: 3, desc: "Notices Board" },
    {
        path: "/api/v1/public/gallery/albums",
        weight: 2,
        desc: "Gallery Albums",
    },
    {
        path: "/api/v1/public/exam-results?standard=8&rollNumber=101&resultType=ANNUAL",
        weight: 5,
        desc: "Results Day Lookup",
    },
];

function calculatePercentiles(latencies) {
    if (latencies.length === 0)
        return { p50: 0, p95: 0, p99: 0, min: 0, max: 0, avg: 0 };
    const sorted = [...latencies].sort((a, b) => a - b);
    const p50 = sorted[Math.floor(sorted.length * 0.5)];
    const p95 = sorted[Math.floor(sorted.length * 0.95)];
    const p99 = sorted[Math.floor(sorted.length * 0.99)];
    const min = sorted[0];
    const max = sorted[sorted.length - 1];
    const avg = (sorted.reduce((acc, v) => acc + v, 0) / sorted.length).toFixed(
        1,
    );
    return { p50, p95, p99, min, max, avg };
}

function makeRequest(url, headers) {
    return new Promise((resolve) => {
        const start = process.hrtime.bigint();
        const parsed = new URL(url);
        const client = parsed.protocol === "https:" ? https : http;

        const req = client.get(url, { headers, timeout: 5000 }, (res) => {
            let data = "";
            res.on("data", (chunk) => {
                data += chunk;
            });
            res.on("end", () => {
                const durationNs = process.hrtime.bigint() - start;
                const durationMs = Number(durationNs) / 1_000_000;
                resolve({
                    statusCode: res.statusCode,
                    durationMs,
                    bytes: Buffer.byteLength(data),
                    success: res.statusCode >= 200 && res.statusCode < 400,
                });
            });
        });

        req.on("error", (err) => {
            const durationNs = process.hrtime.bigint() - start;
            const durationMs = Number(durationNs) / 1_000_000;
            resolve({
                statusCode: 0,
                durationMs,
                bytes: 0,
                success: false,
                error: err.message,
            });
        });

        req.on("timeout", () => {
            req.destroy();
            resolve({
                statusCode: 408,
                durationMs: 5000,
                bytes: 0,
                success: false,
                error: "Timeout",
            });
        });
    });
}

export async function runSimulation() {
    console.log(
        `========================================================================`,
    );
    console.log(
        ` Rorfost School Platform - Capacity & Synthetic Load Simulator (Sprint 59)`,
    );
    console.log(` Target Host      : ${TARGET_HOST}`);
    console.log(` Modeled Schools  : ${SCHOOL_COUNT}`);
    console.log(` Concurrency      : ${CONCURRENCY}`);
    console.log(` Samples / Tenant : ${SAMPLES}`);
    console.log(` Timestamp        : ${new Date().toISOString()}`);
    console.log(
        ` Disclaimer       : Synthetic simulation off-production. 100 rows != 100 active schools.`,
    );
    console.log(
        `========================================================================\n`,
    );

    let totalRequests = 0;
    let successfulRequests = 0;
    const latencies = [];

    // Evaluate across modeled schools
    for (const school of SCHOOLS) {
        for (const scenario of SCENARIOS) {
            const url = `${TARGET_HOST}${scenario.path}`;
            const headers = {
                Accept: "application/json",
                "X-Portal-Gateway-Token": school.token,
                "X-Portal-School-Slug": school.slug,
            };

            for (let s = 0; s < SAMPLES; s++) {
                const result = await makeRequest(url, headers);
                totalRequests++;
                if (result.success) successfulRequests++;
                latencies.push(result.durationMs);
            }
        }
    }

    const stats = calculatePercentiles(latencies);
    console.log(`--- Simulation Results ---`);
    console.log(` Total Requests       : ${totalRequests}`);
    console.log(
        ` Successful Requests  : ${successfulRequests} (${((successfulRequests / totalRequests) * 100).toFixed(1)}%)`,
    );
    console.log(` Latency p50          : ${stats.p50?.toFixed(2)} ms`);
    console.log(` Latency p95          : ${stats.p95?.toFixed(2)} ms`);
    console.log(` Latency p99          : ${stats.p99?.toFixed(2)} ms`);
    console.log(` Average Latency      : ${stats.avg} ms`);
    console.log(
        `========================================================================\n`,
    );

    return { totalRequests, successfulRequests, stats };
}

// Direct CLI invocation
if (process.argv[1]?.endsWith("capacity_simulation.js")) {
    runSimulation().catch((err) => {
        console.error(err);
        process.exit(1);
    });
}
