import { QueryClient } from '@tanstack/react-query';

import { adaptationKeys } from '@/src/features/adaptation/models/adaptationKeys';
import { environmentKeys } from '@/src/features/environments/models/environmentKeys';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export interface OccurrenceEnvironmentScope {
  planId: string;
  dayId: string;
  occurrenceId: string;
}

export async function invalidateAfterEnvironmentMutation(
  queryClient: QueryClient,
  environmentId?: string,
): Promise<void> {
  const tasks: Promise<void>[] = [
    queryClient.invalidateQueries({ queryKey: environmentKeys.all }),
    queryClient.invalidateQueries({ queryKey: ['training', 'overview'] }),
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
  ];
  if (environmentId) {
    tasks.push(queryClient.invalidateQueries({ queryKey: environmentKeys.detail(environmentId) }));
  }
  await Promise.all(tasks);
}

export async function invalidateAfterOccurrenceEnvironmentMutation(
  queryClient: QueryClient,
  scope: OccurrenceEnvironmentScope,
): Promise<void> {
  const { planId, dayId, occurrenceId } = scope;
  // Launch includes feasibility; adaptationKeys.all covers proposals + substitution candidates.
  await Promise.all([
    queryClient.invalidateQueries({
      queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.launch(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
    // Prefix: overview keys are ['training', 'overview', date]
    queryClient.invalidateQueries({ queryKey: ['training', 'overview'] }),
    queryClient.invalidateQueries({ queryKey: adaptationKeys.all }),
  ]);
}
