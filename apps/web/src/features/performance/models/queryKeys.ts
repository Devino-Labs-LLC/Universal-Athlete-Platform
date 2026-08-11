import type { DateOnly } from '@/core/date/dateOnly';
import type { TrainingLoadGranularity } from '@/features/performance/models/schemas';

export interface TrainingLoadHistoryFilters {
  trainingPlanId?: string;
  workoutDayId?: string;
  category?: string;
  movementPattern?: string;
  page?: number;
  size?: number;
}

export const performanceKeys = {
  all: ['performance'] as const,
  recentRecords: (days: number, limit: number) =>
    [...performanceKeys.all, 'recent-records', days, limit] as const,
  personalRecords: (exercisePerformanceKey?: string, recordType?: string) =>
    [...performanceKeys.all, 'personal-records', exercisePerformanceKey ?? null, recordType ?? null] as const,
  exerciseRecords: (exercisePerformanceKey: string) =>
    [...performanceKeys.all, 'exercise-records', exercisePerformanceKey] as const,
  exerciseHistory: (
    exercisePerformanceKey: string,
    filters?: { scheduledFrom?: DateOnly; scheduledTo?: DateOnly; page?: number; size?: number },
  ) =>
    [
      ...performanceKeys.all,
      'exercise-history',
      exercisePerformanceKey,
      filters?.scheduledFrom ?? null,
      filters?.scheduledTo ?? null,
      filters?.page ?? 0,
      filters?.size ?? 20,
    ] as const,
  occurrenceSummary: (planId: string, dayId: string, occurrenceId: string) =>
    [...performanceKeys.all, 'occurrence-summary', planId, dayId, occurrenceId] as const,
  loadHistory: (
    granularity: TrainingLoadGranularity,
    startDate: DateOnly,
    endDate: DateOnly,
    filters?: TrainingLoadHistoryFilters,
  ) =>
    [
      ...performanceKeys.all,
      'load-history',
      granularity,
      startDate,
      endDate,
      filters?.trainingPlanId ?? null,
      filters?.workoutDayId ?? null,
      filters?.category ?? null,
      filters?.movementPattern ?? null,
      filters?.page ?? 0,
      filters?.size ?? 50,
    ] as const,
};
