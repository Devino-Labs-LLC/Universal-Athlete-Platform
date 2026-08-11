import { parseDateOnly } from '@/core/date/dateOnly';
import {
  addDays,
  addMonths,
  daysInMonthGrid,
  isSameMonth,
  monthGridRange,
  startOfMonth,
} from '@/features/training/utils/calendarRange';

describe('calendarRange', () => {
  const month = parseDateOnly('2026-02-01');

  it('computes month grid range with buffer', () => {
    const range = monthGridRange(month, 7);
    expect(range.from).toBe('2026-01-25');
    expect(range.to).toBe('2026-03-07');
  });

  it('builds month grid days', () => {
    const grid = daysInMonthGrid(month);
    expect(grid.length % 7).toBe(0);
    expect(grid.some((date) => date === '2026-02-15')).toBe(true);
  });

  it('adds days without UTC shift', () => {
    expect(addDays(parseDateOnly('2026-02-28'), 1)).toBe('2026-03-01');
  });

  it('adds months from start of month', () => {
    expect(addMonths(startOfMonth(month), 1)).toBe('2026-03-01');
  });

  it('detects same month', () => {
    expect(isSameMonth(parseDateOnly('2026-02-10'), month)).toBe(true);
    expect(isSameMonth(parseDateOnly('2026-03-01'), month)).toBe(false);
  });
});
