import { type DateOnly, parseDateOnly, todayDateOnly } from '@/core/date/dateOnly';

export const RECOVERY_HISTORY_RANGE_OPTIONS = [30, 90] as const;
export type RecoveryHistoryRangeDays = (typeof RECOVERY_HISTORY_RANGE_OPTIONS)[number];

export function isRecoveryHistoryRangeDays(value: number): value is RecoveryHistoryRangeDays {
  return (RECOVERY_HISTORY_RANGE_OPTIONS as readonly number[]).includes(value);
}

export const RECOVERY_HISTORY_MAX_DAYS = 366;
export const RECOVERY_CALENDAR_MAX_DAYS = 93;

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

export function dateRangeForHistory(
  rangeDays: RecoveryHistoryRangeDays,
  endDate: DateOnly = todayDateOnly(),
): { startDate: DateOnly; endDate: DateOnly } {
  return {
    startDate: subtractDays(endDate, rangeDays - 1),
    endDate,
  };
}

export function dateRangeForTrend(rangeDays: number, endDate: DateOnly = todayDateOnly()): {
  startDate: DateOnly;
  endDate: DateOnly;
} {
  return {
    startDate: subtractDays(endDate, rangeDays - 1),
    endDate,
  };
}
