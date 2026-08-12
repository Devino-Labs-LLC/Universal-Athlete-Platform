import { describe, expect, it } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  dateRangeForLoadHistory,
  isLoadRangeDays,
  LOAD_RANGE_OPTIONS,
  subtractDays,
} from '@/features/performance/utils/dateRanges';

describe('isLoadRangeDays', () => {
  it('accepts only 7, 28, and 90', () => {
    for (const option of LOAD_RANGE_OPTIONS) {
      expect(isLoadRangeDays(option)).toBe(true);
    }
    expect(isLoadRangeDays(14)).toBe(false);
  });
});

describe('subtractDays', () => {
  it('subtracts within the month using local calendar dates', () => {
    expect(subtractDays(parseDateOnly('2026-02-15'), 10)).toBe('2026-02-05');
  });

  it('does not shift across UTC midnight boundaries', () => {
    // Local Date construction must keep 2026-08-12 minus 1 day as 2026-08-11.
    expect(subtractDays(parseDateOnly('2026-08-12'), 1)).toBe('2026-08-11');
  });

  it('throws for a malformed date', () => {
    expect(() => subtractDays('bad' as never, 1)).toThrow();
  });
});

describe('dateRangeForLoadHistory', () => {
  it('produces an inclusive range for the 7-day window (7 calendar dates)', () => {
    const range = dateRangeForLoadHistory(7, parseDateOnly('2026-08-12'));
    expect(range.startDate).toBe('2026-08-06');
    expect(range.endDate).toBe('2026-08-12');
  });

  it('produces an inclusive range for the 28-day window (28 calendar dates)', () => {
    const range = dateRangeForLoadHistory(28, parseDateOnly('2026-08-12'));
    expect(range.startDate).toBe('2026-07-16');
    expect(range.endDate).toBe('2026-08-12');
  });

  it('produces an inclusive range for the 90-day window (90 calendar dates)', () => {
    const range = dateRangeForLoadHistory(90, parseDateOnly('2026-08-12'));
    expect(range.startDate).toBe('2026-05-15');
    expect(range.endDate).toBe('2026-08-12');
  });

  it('stays within the backend max history window of 366 inclusive days', () => {
    for (const rangeDays of LOAD_RANGE_OPTIONS) {
      const range = dateRangeForLoadHistory(rangeDays, parseDateOnly('2026-08-12'));
      const start = new Date(
        Number(range.startDate.slice(0, 4)),
        Number(range.startDate.slice(5, 7)) - 1,
        Number(range.startDate.slice(8, 10)),
      );
      const end = new Date(
        Number(range.endDate.slice(0, 4)),
        Number(range.endDate.slice(5, 7)) - 1,
        Number(range.endDate.slice(8, 10)),
      );
      const inclusiveDays = Math.round((end.getTime() - start.getTime()) / 86_400_000) + 1;
      expect(inclusiveDays).toBe(rangeDays);
      expect(inclusiveDays).toBeLessThanOrEqual(366);
    }
  });
});
