import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  fetchSubstitutionCandidates,
  fetchSubstitutionRelationship,
} from '@/features/exercises/api/substitutionsApi';
import { exerciseKeys } from '@/features/exercises/models/queryKeys';
import type { SubstitutionCandidateFilters } from '@/features/exercises/models/schemas';

export function useSubstitutionCandidates(
  sourceId: string,
  filters?: SubstitutionCandidateFilters,
) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: exerciseKeys.candidates(sourceId, filters),
    queryFn: () => fetchSubstitutionCandidates(apiClient, sourceId, filters),
    enabled: status === 'AUTHENTICATED' && Boolean(sourceId),
  });
}

export function useSubstitutionRelationship(relationshipId: string | null) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: exerciseKeys.relationship(relationshipId ?? ''),
    queryFn: () => fetchSubstitutionRelationship(apiClient, relationshipId!),
    enabled: status === 'AUTHENTICATED' && Boolean(relationshipId),
  });
}
