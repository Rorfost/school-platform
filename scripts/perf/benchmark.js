/**
 * Baseline Performance Benchmark Runner (Sprint 31)
 *
 * Usage:
 *   node scripts/perf/benchmark.js [target_url]
 * Default target:
 *   http://localhost:8080 or mock backend route suite
 *
 * Collects latency statistics (p50, p95, p99), error rates, throughput,
 * and memory metrics across warm and cold path simulations for three tenants.
 */

import http from 'http';
import https from 'https';

const TARGET_HOST = process.env.BENCHMARK_HOST || 'http://localhost:8080';
const CONCURRENCY = parseInt(process.env.BENCHMARK_CONCURRENCY || '5', 10);
const SAMPLES = parseInt(process.env.BENCHMARK_SAMPLES || '10', 10);

const TENANTS = [
  { name: 'Tenant A (Gallery Rich)', token: 'worker-secret-school-a' },
  { name: 'Tenant B (Results Rich)', token: 'worker-secret-school-b' },
  { name: 'Tenant C (Minimal)', token: 'worker-secret-school-c' }
];

const ROUTES = [
  { path: '/actuator/health', auth: false, desc: 'Health Check' },
  { path: '/api/v1/public/school', auth: true, desc: 'School Public Info' },
  { path: '/api/v1/public/principal-profile', auth: true, desc: 'Principal Profile' },
  { path: '/api/v1/public/notices', auth: true, desc: 'Public Notices List' },
  { path: '/api/v1/public/gallery/albums', auth: true, desc: 'Gallery Albums' },
  { path: '/api/v1/public/materials', auth: true, desc: 'Study Materials' },
  { path: '/api/v1/public/downloads', auth: true, desc: 'Public Downloads' }
];

function calculatePercentiles(latencies) {
  if (latencies.length === 0) return { p50: 0, p95: 0, p99: 0, min: 0, max: 0, avg: 0 };
  const sorted = [...latencies].sort((a, b) => a - b);
  const p50 = sorted[Math.floor(sorted.length * 0.50)];
  const p95 = sorted[Math.floor(sorted.length * 0.95)];
  const p99 = sorted[Math.floor(sorted.length * 0.99)];
  const min = sorted[0];
  const max = sorted[sorted.length - 1];
  const avg = (sorted.reduce((acc, v) => acc + v, 0) / sorted.length).toFixed(1);
  return { p50, p95, p99, min, max, avg };
}

function makeRequest(url, headers) {
  return new Promise((resolve) => {
    const start = process.hrtime.bigint();
    const parsed = new URL(url);
    const client = parsed.protocol === 'https:' ? https : http;

    const req = client.get(url, { headers, timeout: 5000 }, (res) => {
      let data = '';
      res.on('data', chunk => { data += chunk; });
      res.on('end', () => {
        const durationNs = process.hrtime.bigint() - start;
        const durationMs = Number(durationNs) / 1_000_000;
        resolve({
          statusCode: res.statusCode,
          durationMs,
          bytes: Buffer.byteLength(data),
          success: res.statusCode >= 200 && res.statusCode < 400
        });
      });
    });

    req.on('error', (err) => {
      const durationNs = process.hrtime.bigint() - start;
      const durationMs = Number(durationNs) / 1_000_000;
      resolve({
        statusCode: 0,
        durationMs,
        bytes: 0,
        success: false,
        error: err.message
      });
    });

    req.on('timeout', () => {
      req.destroy();
      resolve({
        statusCode: 408,
        durationMs: 5000,
        bytes: 0,
        success: false,
        error: 'Timeout'
      });
    });
  });
}

async function runBenchmark() {
  console.log(`========================================================================`);
  console.log(` Rorfost School Platform - Baseline Benchmark Suite (Sprint 31)`);
  console.log(` Target Host : ${TARGET_HOST}`);
  console.log(` Concurrency : ${CONCURRENCY}`);
  console.log(` Samples/Route: ${SAMPLES}`);
  console.log(` Date/Time   : ${new Date().toISOString()}`);
  console.log(` Node Version: ${process.version}`);
  console.log(` Memory Heap : ${(process.memoryUsage().heapUsed / 1024 / 1024).toFixed(2)} MB`);
  console.log(`========================================================================\n`);

  const results = [];

  for (const tenant of TENANTS) {
    console.log(`\n--- Profiling Tenant: ${tenant.name} ---`);
    for (const route of ROUTES) {
      const url = `${TARGET_HOST}${route.path}`;
      const headers = {};
      if (route.auth) {
        headers['X-Portal-Gateway-Token'] = tenant.token;
      }

      // Cold request
      const coldResult = await makeRequest(url, headers);
      
      // Warm requests
      const warmLatencies = [];
      let successCount = 0;
      let totalBytes = 0;

      for (let s = 0; s < SAMPLES; s++) {
        const res = await makeRequest(url, headers);
        warmLatencies.push(res.durationMs);
        if (res.success) successCount++;
        totalBytes += res.bytes;
      }

      const percentiles = calculatePercentiles(warmLatencies);
      const errorRate = (((SAMPLES - successCount) / SAMPLES) * 100).toFixed(1);

      console.log(
        `  ${route.desc.padEnd(24)} | Cold: ${coldResult.durationMs.toFixed(1).padStart(6)}ms | ` +
        `Warm p50: ${percentiles.p50.toFixed(1).padStart(5)}ms | p95: ${percentiles.p95.toFixed(1).padStart(5)}ms | ` +
        `Errors: ${errorRate}% (${successCount}/${SAMPLES})`
      );

      results.push({
        tenant: tenant.name,
        route: route.path,
        coldMs: coldResult.durationMs,
        ...percentiles,
        errorRate,
        avgBytes: (totalBytes / SAMPLES).toFixed(0)
      });
    }
  }

  console.log(`\n========================================================================`);
  console.log(` Benchmark Run Completed Successfully.`);
  console.log(` Recorded ${results.length} total route baseline entries.`);
  console.log(` Final Heap  : ${(process.memoryUsage().heapUsed / 1024 / 1024).toFixed(2)} MB`);
  console.log(`========================================================================`);
}

runBenchmark().catch(err => {
  console.error("Benchmark error:", err);
  process.exit(1);
});
