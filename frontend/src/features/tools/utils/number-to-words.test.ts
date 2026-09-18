import { describe, expect, it } from "vitest";
import { numberToEnglishWords } from "./number-to-words";

describe("numberToEnglishWords", () => {
  it.each([
    [0, "zero"],
    [1, "one"],
    [10, "ten"],
    [19, "nineteen"],
    [20, "twenty"],
    [99, "ninety-nine"],
    [100, "one hundred"],
    [101, "one hundred one"],
    [999, "nine hundred ninety-nine"],
    [1000, "one thousand"],
    [100000, "one hundred thousand"],
    [999999, "nine hundred ninety-nine thousand nine hundred ninety-nine"],
  ])("converts %s", (value, expected) => {
    expect(numberToEnglishWords(value)).toBe(expected);
  });

  it("rejects values outside the supported integer range", () => {
    expect(numberToEnglishWords(-1)).toBeNull();
    expect(numberToEnglishWords(1_000_000)).toBeNull();
    expect(numberToEnglishWords(1.5)).toBeNull();
  });
});
