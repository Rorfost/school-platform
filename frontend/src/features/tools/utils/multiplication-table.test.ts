import { describe, expect, it } from "vitest";
import { generateMultiplicationTable } from "./multiplication-table";

describe("generateMultiplicationTable", () => {
  it("generates a normal positive-number table", () => {
    expect(generateMultiplicationTable(7, 1, 3)).toEqual([
      { multiplier: 1, product: 7 },
      { multiplier: 2, product: 14 },
      { multiplier: 3, product: 21 },
    ]);
  });

  it("supports a custom range", () => {
    expect(generateMultiplicationTable(12, 4, 6)).toEqual([
      { multiplier: 4, product: 48 },
      { multiplier: 5, product: 60 },
      { multiplier: 6, product: 72 },
    ]);
  });

  it("rejects invalid ranges", () => {
    expect(generateMultiplicationTable(7, 0, 10)).toBeNull();
    expect(generateMultiplicationTable(7, 10, 9)).toBeNull();
    expect(generateMultiplicationTable(7, 1, 21)).toBeNull();
  });
});
