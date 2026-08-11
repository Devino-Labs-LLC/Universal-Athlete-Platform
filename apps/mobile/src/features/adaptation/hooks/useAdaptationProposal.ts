import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { fetchAdaptationProposal } from '@/src/features/adaptation/api/proposalApi';
import { adaptationKeys } from '@/src/features/adaptation/models/adaptationKeys';

export function useAdaptationProposal(proposalId: string) {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: adaptationKeys.proposal(proposalId),
    queryFn: () => fetchAdaptationProposal(apiClient, proposalId),
    enabled: status === 'AUTHENTICATED' && proposalId.length > 0,
    staleTime: 15_000,
    retry: 1,
  });
}
