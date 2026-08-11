import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchReadinessAssessment } from '@/src/features/recovery/api/readinessApi';
import { recoveryKeys } from '@/src/features/recovery/models/recoveryKeys';

export function useReadinessAssessment(assessmentId: string) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: recoveryKeys.readiness(assessmentId),
    queryFn: () => fetchReadinessAssessment(apiClient, assessmentId),
    enabled: status === 'AUTHENTICATED' && assessmentId.length > 0,
    staleTime: 30_000,
    retry: 1,
  });
}
