const DATE_ONLY_PATTERN = /^(\d{4})-(\d{2})-(\d{2})$/;

export type DateOnly = string & { readonly __brand: 'DateOnly' };

function isValidCalendarDate(year: number, month: number, day: number): boolean {
  if (month < 1 || month > 12 || day < 1) {
    return false;
  }

  const daysInMonth = [31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  const isLeapYear =
    (year % 4 === 0 && year % 100 !== 0) || year % 400 === 0;
  const maxDay = month === 2 && isLeapYear ? 29 : daysInMonth[month - 1];
  return day <= maxDay;
}

export function parseDateOnly(value: string): DateOnly {
  const match = DATE_ONLY_PATTERN.exec(value);
  if (!match) {
    throw new Error(`Invalid date-only value: ${value}`);
  }

  const year = Number(match[1]);
  const month = Number(match[2]);
  const day = Number(match[3]);

  if (!isValidCalendarDate(year, month, day)) {
    throw new Error(`Invalid calendar date: ${value}`);
  }

  return value as DateOnly;
}

export function formatDateOnly(date: DateOnly): string {
  return date;
}

export function todayDateOnly(now = new Date()): DateOnly {
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');
  return parseDateOnly(`${year}-${month}-${day}`);
}
