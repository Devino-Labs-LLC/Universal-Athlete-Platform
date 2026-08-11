import { parseDateOnly } from '@/src/core/date/dateOnly';
import {
  dateRangeForLoadHistory,
  loadRangeDays,
} from '@/src/features/performance/utils/dateRanges';

describe('dateRanges', () => {
  it('returns correct day counts for 7/28/90', () => {
    expect(loadRangeDays('7D')).toBe(7);
    expect(loadRangeDays('28D')).toBe(28);
    expect(loadRangeDays('90D')).toBe(90);
  });

  it('computes inclusive date range ending on provided date', () => {
    const { startDate, endDate } = dateRangeForLoadHistory('7D', parseDateOnly('2026-08-10'));
    expect(endDate).toBe('2026-08-10');
    expect(startDate).toBe('2026-08-04');
  });

  it('computes 28-day range', () => {
    const { startDate, endDate } = dateRangeForLoadHistory('28D', parseDateOnly('2026-08-10'));
    expect(endDate).toBe('2026-08-10');
    expect(startDate).toBe('2026-07-14');
  });
});
