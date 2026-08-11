import { QueryClient } from '@tanstack/react-query';

import { adaptationKeys } from '@/src/features/adaptation/models/adaptationKeys';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import { trainingKeys } from '@/src/features/training/models/queryKeys';

export interface AdaptationOccurrenceScope {
  planId: string;
  dayId: string;
  occurrenceId: string;
  proposalId?: string;
  executionId?: string;
}

export async function invalidateAfterProposalFetch(
  queryClient: QueryClient,
  proposalId: string,
): Promise<void> {
  await queryClient.invalidateQueries({ queryKey: adaptationKeys.proposal(proposalId) });
}

export async function invalidateAfterProposalMutation(
  queryClient: QueryClient,
  scope: AdaptationOccurrenceScope,
): Promise<void> {
  const { planId, dayId, occurrenceId, proposalId } = scope;
  const tasks: Promise<void>[] = [
    queryClient.invalidateQueries({ queryKey: adaptationKeys.list(occurrenceId) }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.launch(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
  ];
  if (proposalId) {
    tasks.push(
      queryClient.invalidateQueries({ queryKey: adaptationKeys.proposal(proposalId) }),
    );
  }
  await Promise.all(tasks);
}

export async function invalidateAfterApplyAdaptation(
  queryClient: QueryClient,
  scope: AdaptationOccurrenceScope,
): Promise<void> {
  const { planId, dayId, occurrenceId, proposalId } = scope;
  await Promise.all([
    proposalId
      ? queryClient.invalidateQueries({ queryKey: adaptationKeys.proposal(proposalId) })
      : Promise.resolve(),
    queryClient.invalidateQueries({ queryKey: adaptationKeys.list(occurrenceId) }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.launch(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.executions(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({ queryKey: ['training', 'calendar'] }),
    queryClient.invalidateQueries({ queryKey: ['training', 'overview'] }),
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
  ]);
}

export async function invalidateAfterDirectSubstitution(
  queryClient: QueryClient,
  scope: AdaptationOccurrenceScope,
): Promise<void> {
  const { planId, dayId, occurrenceId, executionId, proposalId } = scope;
  const tasks: Promise<void>[] = [
    queryClient.invalidateQueries({
      queryKey: trainingKeys.occurrence(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({
      queryKey: trainingKeys.launch(planId, dayId, occurrenceId),
    }),
    queryClient.invalidateQueries({ queryKey: todayQueryKeys.all }),
    queryClient.invalidateQueries({ queryKey: adaptationKeys.list(occurrenceId) }),
  ];
  if (executionId) {
    tasks.push(
      queryClient.invalidateQueries({
        queryKey: adaptationKeys.substitutionHistory(
          planId,
          dayId,
          occurrenceId,
          executionId,
        ),
      }),
      queryClient.invalidateQueries({
        queryKey: adaptationKeys.candidates(planId, dayId, occurrenceId, executionId),
      }),
    );
  }
  if (proposalId) {
    tasks.push(
      queryClient.invalidateQueries({ queryKey: adaptationKeys.proposal(proposalId) }),
    );
  }
  await Promise.all(tasks);
}
