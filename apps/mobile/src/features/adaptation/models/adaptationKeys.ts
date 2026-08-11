export const adaptationKeys = {
  all: ['training', 'adaptation'] as const,
  proposal: (proposalId: string) => ['training', 'adaptation', 'proposal', proposalId] as const,
  list: (occurrenceId?: string, status?: string) =>
    ['training', 'adaptation', 'list', occurrenceId ?? null, status ?? null] as const,
  candidates: (planId: string, dayId: string, occurrenceId: string, executionId: string) =>
    [
      'training',
      'adaptation',
      'candidates',
      planId,
      dayId,
      occurrenceId,
      executionId,
    ] as const,
  substitutionHistory: (
    planId: string,
    dayId: string,
    occurrenceId: string,
    executionId: string,
  ) =>
    [
      'training',
      'adaptation',
      'substitutionHistory',
      planId,
      dayId,
      occurrenceId,
      executionId,
    ] as const,
};
