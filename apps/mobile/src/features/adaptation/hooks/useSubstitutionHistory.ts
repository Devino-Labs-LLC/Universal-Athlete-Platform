import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchSubstitutionHistory } from '@/src/features/adaptation/api/substitutionApi';
import { adaptationKeys } from '@/src/features/adaptation/models/adaptationKeys';

export function useSubstitutionHistory(
  planId: string,
  dayId: string,
  occurrenceId: string,
  executionId: string,
  enabled = true,
) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: adaptationKeys.substitutionHistory(planId, dayId, occurrenceId, executionId),
    queryFn: () =>
      fetchSubstitutionHistory(apiClient, planId, dayId, occurrenceId, executionId),
    enabled:
      enabled &&
      status === 'AUTHENTICATED' &&
      planId.length > 0 &&
      dayId.length > 0 &&
      occurrenceId.length > 0 &&
      executionId.length > 0,
    staleTime: 30_000,
    retry: 1,
  });
}
