import { DateOnly } from '@/src/core/date/dateOnly';
import { TrainingLoadGranularity } from '@/src/features/performance/models/performanceSchemas';

export const performanceKeys = {
  all: ['performance'] as const,
  recentRecords: (days: number, limit: number) =>
    [...performanceKeys.all, 'recent-records', days, limit] as const,
  personalRecords: (exercisePerformanceKey?: string, recordType?: string) =>
    [...performanceKeys.all, 'personal-records', exercisePerformanceKey, recordType] as const,
  exerciseRecords: (exercisePerformanceKey: string) =>
    [...performanceKeys.all, 'exercise-records', exercisePerformanceKey] as const,
  exerciseHistory: (exercisePerformanceKey: string, from?: DateOnly, to?: DateOnly) =>
    [...performanceKeys.all, 'exercise-history', exercisePerformanceKey, from, to] as const,
  occurrenceSummary: (planId: string, dayId: string, occurrenceId: string) =>
    [...performanceKeys.all, 'occurrence-summary', planId, dayId, occurrenceId] as const,
  loadHistory: (
    granularity: TrainingLoadGranularity,
    startDate: DateOnly,
    endDate: DateOnly,
    filters?: { trainingPlanId?: string; page?: number; size?: number },
  ) =>
    [
      ...performanceKeys.all,
      'load-history',
      granularity,
      startDate,
      endDate,
      filters?.trainingPlanId,
      filters?.page,
      filters?.size,
    ] as const,
};
