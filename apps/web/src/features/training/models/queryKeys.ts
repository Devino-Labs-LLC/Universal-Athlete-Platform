import type { DateOnly } from '@/core/date/dateOnly';

export interface PlanListFilters {
  status?: string;
  planType?: string;
}

export interface CalendarQueryFilters {
  status?: string;
  trainingPlanId?: string;
}

export interface ExerciseDefinitionFilters {
  name?: string;
  scope?: string;
  category?: string;
  metricMode?: string;
  page?: number;
  size?: number;
}

export const trainingKeys = {
  all: ['training'] as const,
  overview: (date?: DateOnly) => [...trainingKeys.all, 'overview', date ?? 'today'] as const,
  plans: (filters?: PlanListFilters) =>
    [...trainingKeys.all, 'plans', filters?.status ?? null, filters?.planType ?? null] as const,
  plan: (planId: string) => [...trainingKeys.all, 'plan', planId] as const,
  days: (planId: string) => [...trainingKeys.all, 'days', planId] as const,
  day: (planId: string, dayId: string) => [...trainingKeys.all, 'day', planId, dayId] as const,
  exercises: (planId: string, dayId: string) =>
    [...trainingKeys.all, 'exercises', planId, dayId] as const,
  exerciseDefinitions: (filters?: ExerciseDefinitionFilters) =>
    [
      ...trainingKeys.all,
      'exerciseDefinitions',
      filters?.name ?? null,
      filters?.scope ?? null,
      filters?.category ?? null,
      filters?.metricMode ?? null,
      filters?.page ?? 0,
      filters?.size ?? 20,
    ] as const,
  calendar: (from: DateOnly, to: DateOnly, filters?: CalendarQueryFilters) =>
    [
      ...trainingKeys.all,
      'calendar',
      from,
      to,
      filters?.status ?? null,
      filters?.trainingPlanId ?? null,
    ] as const,
  occurrences: (planId: string, dayId: string) =>
    [...trainingKeys.all, 'occurrences', planId, dayId] as const,
  occurrence: (planId: string, dayId: string, occurrenceId: string) =>
    [...trainingKeys.all, 'occurrence', planId, dayId, occurrenceId] as const,
  environments: () => [...trainingKeys.all, 'environments'] as const,
};
