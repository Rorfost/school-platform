import { describe, expect, it } from "vitest";
import { createQueryKeys } from "./queryKeys";

describe("tenant-safe query key factories", () => {
    it("partitions public and private data by portal origin and by visibility", () => {
        const schoolA = createQueryKeys("https://school-a.example");
        const schoolB = createQueryKeys("https://school-b.example");

        expect(schoolA.school).not.toEqual(schoolB.school);
        expect(schoolA.school).not.toEqual(schoolA.adminSchool);
        expect(schoolA.principalProfile).not.toEqual(schoolA.adminPrincipalProfile);
        expect(schoolA.adminSchool[2]).toBe("admin");
    });

    it("includes pagination and assessment dimensions in the relevant keys", () => {
        const keys = createQueryKeys("https://school-a.example");

        expect(keys.notices({ page: 0, size: 20 })).not.toEqual(
            keys.notices({ page: 1, size: 20 }),
        );
        expect(keys.resultSettings("3", "ANNUAL")).not.toEqual(
            keys.resultSettings("3", "EKAM_KASOTI"),
        );
        expect(keys.resultSettings("3", "ANNUAL")).not.toEqual(keys.resultSettings("4", "ANNUAL"));
    });
});
