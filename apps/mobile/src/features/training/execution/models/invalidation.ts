import { QueryClient } from '@tanstack/react-query';

import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export interface OccurrenceScope {
  planId: string;
  dayId: string;
  occurrenceId: string;
}

export interface ExecutionScope extends OccurrenceScope {
  executionId: string;
}

export async function invalidateOccurrenceQueries(
  queryClient: QueryClient,
  scope: OccurrenceScope,
): Promise<void> {
  const { planId, dayId, occurrenceId } = scope;
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.launch(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({ queryKey: ['training', 'calendar'] }),
    queryClient.invalidateQueries({ queryKey: ['training', 'overview'] }),
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
  ]);
}

export async function invalidateSetValueUpdate(
  queryClient: QueryClient,
  scope: ExecutionScope,
): Promise<void> {
  const { planId, dayId, occurrenceId, executionId } = scope;
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: trainingKeys.sets(planId, dayId, occurrenceId, executionId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    }),
  ]);
}

export async function invalidateSetTerminal(
  queryClient: QueryClient,
  scope: ExecutionScope,
): Promise<void> {
  const { planId, dayId, occurrenceId, executionId } = scope;
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: trainingKeys.sets(planId, dayId, occurrenceId, executionId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.launch(planId, dayId, occurrenceId),
    }),
  ]);
}

export async function invalidateExecutionTerminal(
  queryClient: QueryClient,
  scope: ExecutionScope,
): Promise<void> {
  const { planId, dayId, occurrenceId } = scope;
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.launch(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
  ]);
}

export async function invalidateOccurrenceTerminal(
  queryClient: QueryClient,
  scope: OccurrenceScope,
): Promise<void> {
  const { planId, dayId, occurrenceId } = scope;
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.launch(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({ queryKey: ['training', 'calendar'] }),
    queryClient.invalidateQueries({ queryKey: ['training', 'overview'] }),
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.trainingLoad(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.sessionEffort(planId, dayId, occurrenceId),
    }),
  ]);
}

export async function invalidateStartWorkout(
  queryClient: QueryClient,
  scope: OccurrenceScope,
): Promise<void> {
  await invalidateOccurrenceQueries(queryClient, scope);
}
