/**
 * Performance & Build Asset Budget Verification Gate (Sprint 59)
 *
 * Verifies deterministic build output against production budget ceilings:
 * - Total client JS entry chunk < 450 KB (uncompressed)
 * - Main stylesheet < 75 KB
 * - Warns/fails on unexpected bundle bloat prior to deployment
 */

import fs from "fs";
import path from "path";

const DIST_DIR = fs.existsSync(path.resolve(process.cwd(), "dist/assets"))
    ? path.resolve(process.cwd(), "dist/assets")
    : path.resolve(process.cwd(), "frontend/dist/assets");

const BUDGETS = {
    maxEntryJsKb: 450,
    maxEntryCssKb: 75,
    maxTotalAssetsMb: 4.0,
};

export function checkBuildBudgets() {
    console.log(`Checking build asset budgets in: ${DIST_DIR}`);

    if (!fs.existsSync(DIST_DIR)) {
        console.error(
            `ERROR: Build directory not found: ${DIST_DIR}. Run build first.`,
        );
        process.exit(1);
    }

    const files = fs.readdirSync(DIST_DIR);
    let totalBytes = 0;
    let entryJsBytes = 0;
    let entryCssBytes = 0;

    for (const file of files) {
        const filePath = path.join(DIST_DIR, file);
        const stat = fs.statSync(filePath);
        totalBytes += stat.size;

        if (file.startsWith("index-") && file.endsWith(".js")) {
            entryJsBytes = Math.max(entryJsBytes, stat.size);
        }
        if (file.startsWith("index-") && file.endsWith(".css")) {
            entryCssBytes = Math.max(entryCssBytes, stat.size);
        }
    }

    const entryJsKb = entryJsBytes / 1024;
    const entryCssKb = entryCssBytes / 1024;
    const totalMb = totalBytes / (1024 * 1024);

    console.log(
        `- Entry JS  : ${entryJsKb.toFixed(2)} KB (Budget: ${BUDGETS.maxEntryJsKb} KB)`,
    );
    console.log(
        `- Entry CSS : ${entryCssKb.toFixed(2)} KB (Budget: ${BUDGETS.maxEntryCssKb} KB)`,
    );
    console.log(
        `- Total Dir : ${totalMb.toFixed(2)} MB (Budget: ${BUDGETS.maxTotalAssetsMb} MB)`,
    );

    let failed = false;

    if (entryJsKb > BUDGETS.maxEntryJsKb) {
        console.error(
            `FAILURE: Entry JS exceeds budget (${entryJsKb.toFixed(2)} KB > ${BUDGETS.maxEntryJsKb} KB)`,
        );
        failed = true;
    }

    if (entryCssKb > BUDGETS.maxEntryCssKb) {
        console.error(
            `FAILURE: Entry CSS exceeds budget (${entryCssKb.toFixed(2)} KB > ${BUDGETS.maxEntryCssKb} KB)`,
        );
        failed = true;
    }

    if (totalMb > BUDGETS.maxTotalAssetsMb) {
        console.error(
            `FAILURE: Total assets exceed budget (${totalMb.toFixed(2)} MB > ${BUDGETS.maxTotalAssetsMb} MB)`,
        );
        failed = true;
    }

    if (failed) {
        process.exit(1);
    }

    console.log(`Asset budget verification gate passed successfully.`);
}

if (process.argv[1]?.endsWith("verify_build_budget.js")) {
    checkBuildBudgets();
}
