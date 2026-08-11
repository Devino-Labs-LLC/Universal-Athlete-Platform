import type { DateOnly } from '@/core/date/dateOnly';
import { todayDateOnly } from '@/core/date/dateOnly';

function toLocalDate(date: DateOnly): Date {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function addDays(date: DateOnly, days: number): DateOnly {
  const local = toLocalDate(date);
  local.setDate(local.getDate() + days);
  return todayDateOnly(local);
}

export function startOfMonth(date: DateOnly): DateOnly {
  const local = toLocalDate(date);
  local.setDate(1);
  return todayDateOnly(local);
}

export function endOfMonth(date: DateOnly): DateOnly {
  const local = toLocalDate(date);
  local.setMonth(local.getMonth() + 1, 0);
  return todayDateOnly(local);
}

export function monthGridRange(visibleMonth: DateOnly, bufferDays = 7): { from: DateOnly; to: DateOnly } {
  const monthStart = startOfMonth(visibleMonth);
  const monthEnd = endOfMonth(visibleMonth);
  return {
    from: addDays(monthStart, -bufferDays),
    to: addDays(monthEnd, bufferDays),
  };
}

export function daysInMonthGrid(visibleMonth: DateOnly): DateOnly[] {
  const monthStart = startOfMonth(visibleMonth);
  const monthEnd = endOfMonth(visibleMonth);
  const startOffset = toLocalDate(monthStart).getDay();
  const gridStart = addDays(monthStart, -startOffset);
  const totalDays = startOffset + monthEnd.split('-').map(Number)[2]!;
  const rows = Math.ceil(totalDays / 7) * 7;
  return Array.from({ length: rows }, (_, index) => addDays(gridStart, index));
}

export function addMonths(date: DateOnly, months: number): DateOnly {
  const local = toLocalDate(date);
  local.setMonth(local.getMonth() + months, 1);
  return todayDateOnly(local);
}

export function formatMonthYear(date: DateOnly, locale?: string): string {
  const local = toLocalDate(date);
  return local.toLocaleDateString(locale, { month: 'long', year: 'numeric' });
}

export function isSameMonth(a: DateOnly, b: DateOnly): boolean {
  return a.slice(0, 7) === b.slice(0, 7);
}
