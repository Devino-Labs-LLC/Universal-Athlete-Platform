import { type DateOnly, parseDateOnly, todayDateOnly } from '@/core/date/dateOnly';

export const LOAD_RANGE_OPTIONS = [7, 28, 90] as const;
export type LoadRangeDays = (typeof LOAD_RANGE_OPTIONS)[number];

export function isLoadRangeDays(value: number): value is LoadRangeDays {
  return (LOAD_RANGE_OPTIONS as readonly number[]).includes(value);
}

export function subtractDays(date: DateOnly, days: number): DateOnly {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  if (!match) {
    throw new Error(`Invalid date-only value: ${date}`);
  }
  const parsed = new Date(Number(match[1]), Number(match[2]) - 1, Number(match[3]));
  parsed.setDate(parsed.getDate() - days);
  const year = parsed.getFullYear();
  const month = String(parsed.getMonth() + 1).padStart(2, '0');
  const day = String(parsed.getDate()).padStart(2, '0');
  return parseDateOnly(`${year}-${month}-${day}`);
}

export function dateRangeForLoadHistory(
  rangeDays: LoadRangeDays,
  endDate: DateOnly = todayDateOnly(),
): { startDate: DateOnly; endDate: DateOnly } {
  return {
    startDate: subtractDays(endDate, rangeDays - 1),
    endDate,
  };
}
