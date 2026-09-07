import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/src/app/providers/AuthSessionProvider';
import { listMyInvitations } from '@/src/features/organization/api/invitationsApi';
import { invitationKeys } from '@/src/features/organization/models/queryKeys';

export function useMyInvitations() {
  const { apiClient, status } = useAuthSession();

  return useQuery({
    queryKey: invitationKeys.mine(),
    queryFn: () => listMyInvitations(apiClient),
    enabled: status === 'AUTHENTICATED',
    staleTime: 30_000,
    retry: 1,
  });
}
