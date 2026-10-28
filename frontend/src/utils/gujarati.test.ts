import { describe, expect, it } from "vitest";
import { LABELS, toGujaratiNumber } from "./gujarati";

describe("toGujaratiNumber", () => {
    it("converts single digits correctly", () => {
        expect(toGujaratiNumber(0)).toBe("૦");
        expect(toGujaratiNumber(1)).toBe("૧");
        expect(toGujaratiNumber(5)).toBe("૫");
        expect(toGujaratiNumber(9)).toBe("૯");
    });

    it("converts multi-digit strings and numbers", () => {
        expect(toGujaratiNumber(2024)).toBe("૨૦૨૪");
        expect(toGujaratiNumber("24010101234")).toBe("૨૪૦૧૦૧૦૧૨૩૪");
    });

    it("preserves non-digit characters and formatting", () => {
        expect(toGujaratiNumber("ધોરણ-8 (વર્ગ A)")).toBe("ધોરણ-૮ (વર્ગ A)");
        expect(toGujaratiNumber("Date: 15-08-1947")).toBe("Date: ૧૫-૦૮-૧૯૪૭");
    });

    it("handles null, undefined, or empty inputs gracefully", () => {
        expect(toGujaratiNumber(null)).toBe("");
        expect(toGujaratiNumber(undefined)).toBe("");
        expect(toGujaratiNumber("")).toBe("");
    });
});

describe("LABELS dictionary", () => {
    it("contains consistent Gujarati navigation labels", () => {
        expect(LABELS.home).toBe("હોમ");
        expect(LABELS.about).toBe("શાળા વિશે");
        expect(LABELS.studentCorner).toBe("વિદ્યાર્થી વિભાગ");
        expect(LABELS.results).toBe("પરીક્ષા પરિણામ");
    });

    it("defines standard Annual and Trimasik assessment terms", () => {
        expect(LABELS.annualExam).toBe("વાર્ષિક પરીક્ષા");
        expect(LABELS.trimasikKasoti).toBe("ત્રિમાસિક કસોટી");
        expect(LABELS.annualExamResult).toBe("વાર્ષિક પરીક્ષા પરિણામ");
        expect(LABELS.trimasikKasotiResult).toBe("ત્રિમાસિક કસોટી પરિણામ");
    });

    it("provides clean empty state and error messages", () => {
        expect(LABELS.loading).toBe("લોડ થઈ રહ્યું છે...");
        expect(LABELS.empty).toBe("હાલ કોઈ માહિતી ઉપલબ્ધ નથી.");
        expect(LABELS.tryAgain).toBe("ફરી પ્રયત્ન કરો");
        expect(LABELS.errorFallback).toContain("હમણાં માહિતી મળી શકી નથી");
    });
});
