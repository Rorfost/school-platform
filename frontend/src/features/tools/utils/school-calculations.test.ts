import { describe, expect, it } from "vitest";
import {
  calculateAttendance,
  calculateMarks,
  calculateMultiSubjectMarks,
  formatPercentage,
} from "./school-calculations";

describe("school calculations", () => {
  it("calculates attendance and formats decimal percentages", () => {
    expect(calculateAttendance(220, 205)).toEqual({
      absentDays: 15,
      percentage: 93.18181818181817,
    });
    expect(formatPercentage(calculateAttendance(220, 205)?.percentage ?? 0)).toBe("93.18%");
    expect(calculateAttendance(10, 10)).toMatchObject({ percentage: 100 });
    expect(calculateAttendance(10, 0)).toMatchObject({ percentage: 0 });
  });

  it("rejects invalid attendance values", () => {
    expect(calculateAttendance(0, 0)).toBeNull();
    expect(calculateAttendance(10, 11)).toBeNull();
    expect(calculateAttendance(10.5, 5)).toBeNull();
  });

  it("calculates marks and rejects invalid ranges", () => {
    expect(calculateMarks(45, 50)).toEqual({ percentage: 90 });
    expect(calculateMarks(100, 100)).toEqual({ percentage: 100 });
    expect(calculateMarks(0, 50)).toEqual({ percentage: 0 });
    expect(calculateMarks(51, 50)).toBeNull();
    expect(calculateMarks(0, 0)).toBeNull();
  });

  it("calculates multi-subject totals", () => {
    expect(
      calculateMultiSubjectMarks([
        { obtained: 40, maximum: 50 },
        { obtained: 30, maximum: 40 },
      ]),
    ).toEqual({
      totalObtained: 70,
      totalMaximum: 90,
      overallPercentage: 77.77777777777779,
      averagePercentage: 77.5,
    });
  });
});
