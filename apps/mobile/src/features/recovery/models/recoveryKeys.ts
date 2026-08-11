import { DateOnly } from '@/src/core/date/dateOnly';

export type TrendDays = 7 | 14 | 28;

export const recoveryKeys = {
  all: ['recovery'] as const,
  overview: (date: DateOnly, trendDays: TrendDays) =>
    ['recovery', 'overview', date, trendDays] as const,
  overviewPrefix: () => ['recovery', 'overview'] as const,
  checkInByDate: (date: DateOnly) => ['recovery', 'check-in', 'by-date', date] as const,
  checkIn: (id: string) => ['recovery', 'check-in', id] as const,
  history: (start: DateOnly, end: DateOnly, includeLoad: boolean) =>
    ['recovery', 'history', start, end, includeLoad] as const,
  readiness: (assessmentId: string) => ['recovery', 'readiness', assessmentId] as const,
  recommendation: (recommendationId: string) =>
    ['recovery', 'recommendation', recommendationId] as const,
};
