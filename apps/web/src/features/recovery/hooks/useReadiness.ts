import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import type { DateOnly } from '@/core/date/dateOnly';
import {
  fetchReadinessAssessment,
  fetchReadinessComparison,
  fetchReadinessHistory,
} from '@/features/recovery/api/readinessApi';
import { recoveryKeys } from '@/features/recovery/models/queryKeys';

export function useReadinessAssessment(assessmentId: string | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.readiness(assessmentId ?? ''),
    queryFn: () => fetchReadinessAssessment(apiClient, assessmentId as string),
    enabled: status === 'AUTHENTICATED' && Boolean(assessmentId),
  });
}

export function useReadinessHistory(startDate: DateOnly, endDate: DateOnly) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.readinessHistory(startDate, endDate),
    queryFn: () => fetchReadinessHistory(apiClient, startDate, endDate),
    enabled: status === 'AUTHENTICATED' && Boolean(startDate) && Boolean(endDate),
  });
}

export function useReadinessComparison(olderAssessmentId: string | undefined, newerAssessmentId: string | undefined) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: recoveryKeys.readinessCompare(olderAssessmentId ?? '', newerAssessmentId ?? ''),
    queryFn: () => fetchReadinessComparison(apiClient, olderAssessmentId as string, newerAssessmentId as string),
    enabled: status === 'AUTHENTICATED' && Boolean(olderAssessmentId) && Boolean(newerAssessmentId),
  });
}
