import { parseDateOnly, todayDateOnly } from '@/src/core/date/dateOnly';

describe('dateOnly', () => {
  it('parses valid YYYY-MM-DD values', () => {
    expect(parseDateOnly('2026-08-10')).toBe('2026-08-10');
  });

  it('rejects invalid calendar dates', () => {
    expect(() => parseDateOnly('2026-02-30')).toThrow(/Invalid calendar date/);
  });

  it('rejects malformed strings', () => {
    expect(() => parseDateOnly('08-10-2026')).toThrow(/Invalid date-only value/);
  });

  it('builds today from local calendar fields', () => {
    const now = new Date(2026, 7, 10, 15, 30, 0);
    expect(todayDateOnly(now)).toBe('2026-08-10');
  });
});
