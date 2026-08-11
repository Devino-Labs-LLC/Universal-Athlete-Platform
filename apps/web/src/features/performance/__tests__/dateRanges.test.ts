import { describe, expect, it } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import { dateRangeForLoadHistory, isLoadRangeDays, LOAD_RANGE_OPTIONS, subtractDays } from '@/features/performance/utils/dateRanges';

describe('isLoadRangeDays', () => {
  it('accepts only 7, 28, and 90', () => {
    for (const option of LOAD_RANGE_OPTIONS) {
      expect(isLoadRangeDays(option)).toBe(true);
    }
    expect(isLoadRangeDays(14)).toBe(false);
  });
});

describe('subtractDays', () => {
  it('subtracts within the month', () => {
    expect(subtractDays(parseDateOnly('2026-02-15'), 10)).toBe('2026-02-05');
  });

  it('throws for a malformed date', () => {
    expect(() => subtractDays('bad' as never, 1)).toThrow();
  });
});

describe('dateRangeForLoadHistory', () => {
  it('produces an inclusive range for the 7-day window', () => {
    const range = dateRangeForLoadHistory(7, parseDateOnly('2026-02-07'));
    expect(range.startDate).toBe('2026-02-01');
    expect(range.endDate).toBe('2026-02-07');
  });

  it('produces an inclusive range for the 90-day window', () => {
    const range = dateRangeForLoadHistory(90, parseDateOnly('2026-03-31'));
    expect(range.startDate).toBe('2026-01-01');
  });
});
