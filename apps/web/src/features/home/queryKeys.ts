import type { DateOnly } from '@/core/date/dateOnly';
import { todayQueryDate } from '@/features/home/schemas';

export const trainingClientKeys = {
  all: ['training-client'] as const,
  bootstrap: () => [...trainingClientKeys.all, 'bootstrap'] as const,
  today: (date?: DateOnly) =>
    [...trainingClientKeys.all, 'today', todayQueryDate(date)] as const,
};
