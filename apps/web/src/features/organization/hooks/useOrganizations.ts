import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  fetchMyOrganizations,
  fetchOrganizationTeams,
} from '@/features/organization/api/organizationsApi';
import { organizationKeys } from '@/features/organization/models/queryKeys';

export function useMyOrganizations() {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: organizationKeys.organizationList(),
    queryFn: () => fetchMyOrganizations(apiClient),
    enabled: status === 'AUTHENTICATED',
  });
}

export function useOrganizationTeams(organizationId: string | null) {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: organizationKeys.teamList(organizationId ?? ''),
    queryFn: () => fetchOrganizationTeams(apiClient, organizationId!),
    enabled: status === 'AUTHENTICATED' && Boolean(organizationId),
  });
}
