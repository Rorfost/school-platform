import { describe, expect, it } from "vitest";
import { calculateCalendarDifference, getDayOfWeek, parseCalendarDate } from "./date-calculations";

describe("calculateCalendarDifference", () => {
  it("calculates a normal age using calendar units", () => {
    expect(calculateCalendarDifference("2018-03-05", "2024-06-17")).toEqual({
      years: 6,
      months: 3,
      days: 12,
      totalMonths: 75,
      totalDays: 2296,
    });
  });

  it("handles a birthday, the day before, and the day after", () => {
    expect(calculateCalendarDifference("2018-06-15", "2024-06-15")).toMatchObject({ years: 6, months: 0, days: 0 });
    expect(calculateCalendarDifference("2018-06-15", "2024-06-14")).toMatchObject({ years: 5, months: 11, days: 30 });
    expect(calculateCalendarDifference("2018-06-15", "2024-06-16")).toMatchObject({ years: 6, months: 0, days: 1 });
  });

  it("clamps leap-day birthdays to February 28 in non-leap years", () => {
    expect(calculateCalendarDifference("2020-02-29", "2021-02-28")).toMatchObject({ years: 1, months: 0, days: 0 });
    expect(calculateCalendarDifference("2020-02-29", "2024-02-29")).toMatchObject({ years: 4, months: 0, days: 0 });
  });

  it("handles month ends and year boundaries", () => {
    expect(calculateCalendarDifference("2023-01-31", "2023-02-28")).toMatchObject({ years: 0, months: 1, days: 0 });
    expect(calculateCalendarDifference("2023-12-31", "2024-01-01")).toMatchObject({ years: 0, months: 0, days: 1 });
  });

  it("returns zero for the same day and rejects reversed or invalid dates", () => {
    expect(calculateCalendarDifference("2024-01-01", "2024-01-01")).toMatchObject({ years: 0, months: 0, days: 0, totalDays: 0 });
    expect(calculateCalendarDifference("2024-01-02", "2024-01-01")).toBeNull();
    expect(parseCalendarDate("2024-02-30")).toBeNull();
  });

  it("shows the selected date day of week without timezone shifting", () => {
    expect(getDayOfWeek("2020-06-15")).toBe("સોમવાર");
  });
});
