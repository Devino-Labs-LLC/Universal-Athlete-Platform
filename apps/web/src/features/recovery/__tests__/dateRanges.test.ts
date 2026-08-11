import { describe, expect, it } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  dateRangeForHistory,
  dateRangeForTrend,
  isRecoveryHistoryRangeDays,
  RECOVERY_CALENDAR_MAX_DAYS,
  RECOVERY_HISTORY_MAX_DAYS,
  subtractDays,
} from '@/features/recovery/utils/dateRanges';

describe('subtractDays', () => {
  it('subtracts days within the same month', () => {
    expect(subtractDays(parseDateOnly('2026-02-10'), 5)).toBe('2026-02-05');
  });

  it('rolls back across a month boundary', () => {
    expect(subtractDays(parseDateOnly('2026-03-01'), 1)).toBe('2026-02-28');
  });

  it('rolls back across a year boundary', () => {
    expect(subtractDays(parseDateOnly('2026-01-01'), 1)).toBe('2025-12-31');
  });

  it('throws for an invalid date-only value', () => {
    expect(() => subtractDays('not-a-date' as never, 1)).toThrow();
  });
});

describe('isRecoveryHistoryRangeDays', () => {
  it('accepts only 30 and 90', () => {
    expect(isRecoveryHistoryRangeDays(30)).toBe(true);
    expect(isRecoveryHistoryRangeDays(90)).toBe(true);
    expect(isRecoveryHistoryRangeDays(60)).toBe(false);
  });
});

describe('dateRangeForHistory', () => {
  it('produces an inclusive range spanning rangeDays', () => {
    const range = dateRangeForHistory(30, parseDateOnly('2026-02-28'));
    expect(range.endDate).toBe('2026-02-28');
    expect(range.startDate).toBe('2026-01-30');
  });

  it('supports the 90-day range', () => {
    const range = dateRangeForHistory(90, parseDateOnly('2026-03-31'));
    expect(range.startDate).toBe('2026-01-01');
  });
});

describe('dateRangeForTrend', () => {
  it('produces an inclusive range for an arbitrary window', () => {
    const range = dateRangeForTrend(28, parseDateOnly('2026-02-28'));
    expect(range.endDate).toBe('2026-02-28');
    expect(range.startDate).toBe('2026-02-01');
  });
});

describe('documented backend limits', () => {
  it('exposes the calendar and history max-day constants', () => {
    expect(RECOVERY_CALENDAR_MAX_DAYS).toBe(93);
    expect(RECOVERY_HISTORY_MAX_DAYS).toBe(366);
  });
});
