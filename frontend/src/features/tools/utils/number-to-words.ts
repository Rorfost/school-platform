const BELOW_TWENTY = [
  "zero",
  "one",
  "two",
  "three",
  "four",
  "five",
  "six",
  "seven",
  "eight",
  "nine",
  "ten",
  "eleven",
  "twelve",
  "thirteen",
  "fourteen",
  "fifteen",
  "sixteen",
  "seventeen",
  "eighteen",
  "nineteen",
];

const TENS = ["", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety"];

export function numberToEnglishWords(value: number): string | null {
  if (!Number.isInteger(value) || value < 0 || value > 999_999) return null;
  if (value < 1000) return underOneThousand(value);

  const thousands = Math.floor(value / 1000);
  const remainder = value % 1000;
  return `${underOneThousand(thousands)} thousand${remainder ? ` ${underOneThousand(remainder)}` : ""}`;
}

function underOneThousand(value: number): string {
  if (value < 20) return BELOW_TWENTY[value] ?? "";
  if (value < 100) {
    const tens = TENS[Math.floor(value / 10)] ?? "";
    return value % 10 ? `${tens}-${BELOW_TWENTY[value % 10]}` : tens;
  }

  const hundreds = `${BELOW_TWENTY[Math.floor(value / 100)]} hundred`;
  return value % 100 ? `${hundreds} ${underOneThousand(value % 100)}` : hundreds;
}
