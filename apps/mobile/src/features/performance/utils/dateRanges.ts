import { DateOnly, parseDateOnly, todayDateOnly } from '@/src/core/date/dateOnly';

export type LoadRangeKey = '7D' | '28D' | '90D';

const RANGE_DAYS: Record<LoadRangeKey, number> = {
  '7D': 7,
  '28D': 28,
  '90D': 90,
};

function subtractDays(date: DateOnly, days: number): DateOnly {
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

export function loadRangeDays(range: LoadRangeKey): number {
  return RANGE_DAYS[range];
}

export function dateRangeForLoadHistory(
  range: LoadRangeKey,
  endDate: DateOnly = todayDateOnly(),
): { startDate: DateOnly; endDate: DateOnly } {
  const days = loadRangeDays(range);
  return {
    startDate: subtractDays(endDate, days - 1),
    endDate,
  };
}
