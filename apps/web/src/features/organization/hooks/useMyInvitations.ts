import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import { fetchMyInvitations } from '@/features/organization/api/invitationsApi';
import { organizationKeys } from '@/features/organization/models/queryKeys';

export function useMyInvitations() {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: organizationKeys.myInvitations(),
    queryFn: () => fetchMyInvitations(apiClient),
    enabled: status === 'AUTHENTICATED',
  });
}
