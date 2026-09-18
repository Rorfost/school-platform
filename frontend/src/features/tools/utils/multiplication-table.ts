export interface MultiplicationRow {
  multiplier: number;
  product: number;
}

export function generateMultiplicationTable(
  value: number,
  from: number,
  to: number,
): MultiplicationRow[] | null {
  if (
    !Number.isInteger(value) ||
    value <= 0 ||
    !Number.isInteger(from) ||
    !Number.isInteger(to) ||
    from < 1 ||
    to > 20 ||
    from > to
  ) {
    return null;
  }

  return Array.from({ length: to - from + 1 }, (_, index) => {
    const multiplier = from + index;
    return { multiplier, product: value * multiplier };
  });
}
