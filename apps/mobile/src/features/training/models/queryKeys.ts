import { DateOnly } from '@/src/core/date/dateOnly';

export interface CalendarQueryFilters {
  status?: string;
  trainingPlanId?: string;
}

export const trainingKeys = {
  overview: (date?: DateOnly) => ['training', 'overview', date ?? 'today'] as const,
  calendar: (from: DateOnly, to: DateOnly, filters?: CalendarQueryFilters) =>
    ['training', 'calendar', from, to, filters?.status ?? null, filters?.trainingPlanId ?? null] as const,
  plan: (planId: string) => ['training', 'plan', planId] as const,
  planDays: (planId: string) => ['training', 'planDays', planId] as const,
  dayExercises: (planId: string, dayId: string) =>
    ['training', 'dayExercises', planId, dayId] as const,
  occurrence: (planId: string, dayId: string, occurrenceId: string) =>
    ['training', 'occurrence', planId, dayId, occurrenceId] as const,
  launch: (planId: string, dayId: string, occurrenceId: string) =>
    ['training', 'launch', planId, dayId, occurrenceId] as const,
};
