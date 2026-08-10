import { parseDateOnly } from '@/src/core/date/dateOnly';
import {
  addDays,
  endOfWeek,
  startOfWeek,
  weekDates,
} from '@/src/features/training/utils/calendarRange';

describe('calendarRange helpers', () => {
  it('computes week boundaries without UTC shift', () => {
    const date = parseDateOnly('2026-08-10'); // Monday when week starts Sunday
    const weekStart = startOfWeek(date, 0);
    expect(weekStart).toBe('2026-08-09');
    expect(endOfWeek(weekStart)).toBe('2026-08-15');
  });

  it('adds days using local calendar arithmetic', () => {
    const date = parseDateOnly('2026-08-10');
    expect(addDays(date, 1)).toBe('2026-08-11');
    expect(addDays(date, -1)).toBe('2026-08-09');
  });

  it('returns seven consecutive week dates', () => {
    const weekStart = parseDateOnly('2026-08-04');
    expect(weekDates(weekStart)).toEqual([
      '2026-08-04',
      '2026-08-05',
      '2026-08-06',
      '2026-08-07',
      '2026-08-08',
      '2026-08-09',
      '2026-08-10',
    ]);
  });
});
