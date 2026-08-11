import { describe, expect, it } from 'vitest';

import {
  formatDateDisplay,
  parseDateOnly,
  todayDateOnly,
} from '@/core/date/dateOnly';

describe('dateOnly', () => {
  it('parses valid calendar dates', () => {
    expect(parseDateOnly('2026-08-11')).toBe('2026-08-11');
  });

  it('rejects invalid calendar dates', () => {
    expect(() => parseDateOnly('2026-02-30')).toThrow(/Invalid calendar date/);
  });

  it('formats display using local calendar parts without UTC shift', () => {
    const dateOnly = parseDateOnly('2026-08-11');
    const formatted = formatDateDisplay(dateOnly, 'en-US');
    expect(formatted).toContain('Aug');
    expect(formatted).toContain('11');
    expect(formatted).toContain('2026');
  });

  it('derives today from local date parts', () => {
    const today = todayDateOnly(new Date(2026, 7, 11, 23, 59, 59));
    expect(today).toBe('2026-08-11');
  });
});
