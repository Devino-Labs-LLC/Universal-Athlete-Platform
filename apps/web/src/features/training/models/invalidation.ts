import type { QueryClient } from '@tanstack/react-query';

import { trainingKeys } from '@/features/training/models/queryKeys';

export function invalidatePlanQueries(queryClient: QueryClient, planId: string): void {
  void queryClient.invalidateQueries({ queryKey: trainingKeys.plan(planId) });
  void queryClient.invalidateQueries({ queryKey: trainingKeys.plans() });
  void queryClient.invalidateQueries({ queryKey: trainingKeys.overview() });
}

export function invalidateDayQueries(queryClient: QueryClient, planId: string, dayId?: string): void {
  void queryClient.invalidateQueries({ queryKey: trainingKeys.days(planId) });
  if (dayId) {
    void queryClient.invalidateQueries({ queryKey: trainingKeys.day(planId, dayId) });
  }
  void queryClient.invalidateQueries({ queryKey: trainingKeys.plan(planId) });
}

export function invalidateExerciseQueries(
  queryClient: QueryClient,
  planId: string,
  dayId: string,
): void {
  void queryClient.invalidateQueries({ queryKey: trainingKeys.exercises(planId, dayId) });
}

export function invalidateScheduleQueries(queryClient: QueryClient, planId: string): void {
  void queryClient.invalidateQueries({ queryKey: trainingKeys.plan(planId) });
  void queryClient.invalidateQueries({ queryKey: trainingKeys.plans() });
  void queryClient.invalidateQueries({ queryKey: trainingKeys.overview() });
}

export function invalidateCalendarQueries(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: trainingKeys.all });
}

export function invalidateOccurrenceQueries(
  queryClient: QueryClient,
  planId: string,
  dayId: string,
  occurrenceId?: string,
): void {
  void queryClient.invalidateQueries({ queryKey: trainingKeys.occurrences(planId, dayId) });
  if (occurrenceId) {
    void queryClient.invalidateQueries({
      queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    });
  }
  void queryClient.invalidateQueries({ queryKey: trainingKeys.all });
}
