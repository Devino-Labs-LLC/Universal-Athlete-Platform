import { useQueries, UseQueryResult } from '@tanstack/react-query';

import { useAuthSession } from '@/src/providers/AuthSessionProvider';
import { fetchExerciseSets } from '@/src/features/training/execution/api/setApi';
import {
  ExerciseExecutionDetail,
  WorkoutExerciseSet,
} from '@/src/features/training/execution/models/executionSchemas';
import { useOccurrenceDetail } from '@/src/features/training/hooks/useOccurrenceDetail';
import { ExerciseExecution } from '@/src/features/training/models/browseSchemas';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

type SetsQueryResult = UseQueryResult<WorkoutExerciseSet[], Error>;

export function useWorkoutExecution(planId: string, dayId: string, occurrenceId: string) {
  const { apiClient, status } = useAuthSession();
  const occurrenceQuery = useOccurrenceDetail(planId, dayId, occurrenceId);

  const executions: ExerciseExecution[] = [...(occurrenceQuery.data?.executions ?? [])].sort(
    (a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0),
  );

  const setsQueries = useQueries({
    queries: executions.map((execution) => ({
      queryKey: trainingKeys.sets(planId, dayId, occurrenceId, execution.id),
      queryFn: () => fetchExerciseSets(apiClient, planId, dayId, occurrenceId, execution.id),
      enabled:
        status === 'AUTHENTICATED' &&
        Boolean(planId) &&
        Boolean(dayId) &&
        Boolean(occurrenceId) &&
        Boolean(execution.id) &&
        (occurrenceQuery.data?.status === 'IN_PROGRESS' ||
          occurrenceQuery.data?.status === 'COMPLETED'),
      staleTime: 15_000,
      retry: 1,
    })),
  }) as SetsQueryResult[];

  const executionSetsMap = new Map<string, SetsQueryResult>();
  executions.forEach((execution, index) => {
    executionSetsMap.set(execution.id, setsQueries[index]);
  });

  return {
    occurrenceQuery,
    executions,
    executionSetsMap,
    getSetsForExecution: (executionId: string) => executionSetsMap.get(executionId),
  };
}

export function sortExecutions(executions: ExerciseExecutionDetail[]): ExerciseExecutionDetail[] {
  return [...executions].sort((a, b) => (a.displayOrder ?? 0) - (b.displayOrder ?? 0));
}
