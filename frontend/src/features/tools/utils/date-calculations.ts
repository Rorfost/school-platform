export interface CalendarDate {
  year: number;
  month: number;
  day: number;
}

export interface CalendarDifference {
  years: number;
  months: number;
  days: number;
  totalMonths: number;
  totalDays: number;
}

const DATE_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;
const GUJARATI_DAYS = ["રવિવાર", "સોમવાર", "મંગળવાર", "બુધવાર", "ગુરુવાર", "શુક્રવાર", "શનિવાર"];

export function parseCalendarDate(value: string): CalendarDate | null {
  const match = DATE_PATTERN.exec(value);
  if (!match) return null;

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);
  const date = new Date(year, month - 1, day);

  return date.getFullYear() === year && date.getMonth() === month - 1 && date.getDate() === day
    ? { year, month, day }
    : null;
}

export function formatCalendarDate(date: CalendarDate): string {
  return `${date.year}-${String(date.month).padStart(2, "0")}-${String(date.day).padStart(2, "0")}`;
}

export function todayCalendarDate(): string {
  const now = new Date();
  return formatCalendarDate({
    year: now.getFullYear(),
    month: now.getMonth() + 1,
    day: now.getDate(),
  });
}

export function compareCalendarDates(left: CalendarDate, right: CalendarDate): number {
  return toOrdinal(left) - toOrdinal(right);
}

export function calculateCalendarDifference(
  startValue: string,
  endValue: string,
): CalendarDifference | null {
  const start = parseCalendarDate(startValue);
  const end = parseCalendarDate(endValue);
  if (!start || !end || compareCalendarDates(start, end) > 0) return null;

  let years = end.year - start.year;
  if (compareCalendarDates(addYearsClamped(start, years), end) > 0) years -= 1;

  const afterYears = addYearsClamped(start, years);
  let months = (end.year - afterYears.year) * 12 + (end.month - afterYears.month);
  if (compareCalendarDates(addMonthsClamped(afterYears, months), end) > 0) months -= 1;

  const afterMonths = addMonthsClamped(afterYears, months);
  const days = toOrdinal(end) - toOrdinal(afterMonths);

  return {
    years,
    months,
    days,
    totalMonths: years * 12 + months,
    totalDays: toOrdinal(end) - toOrdinal(start),
  };
}

export function getDayOfWeek(value: string): string | null {
  const date = parseCalendarDate(value);
  if (!date) return null;
  return GUJARATI_DAYS[new Date(date.year, date.month - 1, date.day).getDay()] ?? null;
}

function addYearsClamped(date: CalendarDate, years: number): CalendarDate {
  return createClampedDate(date.year + years, date.month, date.day);
}

function addMonthsClamped(date: CalendarDate, months: number): CalendarDate {
  const monthIndex = date.month - 1 + months;
  const year = date.year + Math.floor(monthIndex / 12);
  const month = (((monthIndex % 12) + 12) % 12) + 1;
  return createClampedDate(year, month, date.day);
}

function createClampedDate(year: number, month: number, day: number): CalendarDate {
  return { year, month, day: Math.min(day, daysInMonth(year, month)) };
}

function daysInMonth(year: number, month: number): number {
  return new Date(year, month, 0).getDate();
}

function toOrdinal(date: CalendarDate): number {
  return Math.floor(Date.UTC(date.year, date.month - 1, date.day) / 86_400_000);
}
