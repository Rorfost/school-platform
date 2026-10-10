import { describe, expect, it } from "vitest";
import { formatDate } from "./date";

describe("formatDate", () => {
  it("formats dates with various delimiters into strict DD/MM/YYYY", () => {
    expect(formatDate("15.08.2012")).toBe("15/08/2012");
    expect(formatDate("5.3.18")).toBe("05/03/2018");
    expect(formatDate("01-05-2015")).toBe("01/05/2015");
    expect(formatDate("15/08/2010")).toBe("15/08/2010");
    expect(formatDate("2010-08-15")).toBe("15/08/2010");
    expect(formatDate("5 3 2018")).toBe("05/03/2018");
  });

  it("handles null, undefined, empty, and invalid inputs gracefully", () => {
    expect(formatDate(null)).toBe("-");
    expect(formatDate(undefined)).toBe("-");
    expect(formatDate("")).toBe("-");
  });
});
