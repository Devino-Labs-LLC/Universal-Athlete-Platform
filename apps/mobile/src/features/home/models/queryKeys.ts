import { DateOnly } from '@/src/core/date/dateOnly';

export const todayQueryKeys = {
  all: ['training', 'today'] as const,
  date: (date?: DateOnly) =>
    date ? (['training', 'today', date] as const) : (['training', 'today', 'current'] as const),
};
