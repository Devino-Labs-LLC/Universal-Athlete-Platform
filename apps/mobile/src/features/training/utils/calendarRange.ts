import { DateOnly, todayDateOnly } from '@/src/core/date/dateOnly';

function toLocalDate(date: DateOnly): Date {
  const [year, month, day] = date.split('-').map(Number);
  return new Date(year, month - 1, day);
}

export function startOfWeek(date: DateOnly, weekStartsOn: 0 | 1 = 0): DateOnly {
  const local = toLocalDate(date);
  const day = local.getDay();
  const diff = (day - weekStartsOn + 7) % 7;
  local.setDate(local.getDate() - diff);
  return todayDateOnly(local);
}

export function endOfWeek(weekStart: DateOnly): DateOnly {
  return addDays(weekStart, 6);
}

export function addDays(date: DateOnly, days: number): DateOnly {
  const local = toLocalDate(date);
  local.setDate(local.getDate() + days);
  return todayDateOnly(local);
}

export function weekDates(weekStart: DateOnly): DateOnly[] {
  return Array.from({ length: 7 }, (_, index) => addDays(weekStart, index));
}

export function formatWeekdayShort(date: DateOnly): string {
  const local = toLocalDate(date);
  return local.toLocaleDateString(undefined, { weekday: 'short' });
}

export function formatDayOfMonth(date: DateOnly): string {
  const local = toLocalDate(date);
  return String(local.getDate());
}
