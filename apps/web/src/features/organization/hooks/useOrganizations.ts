import { useQuery } from '@tanstack/react-query';

import { useAuthSession } from '@/app/providers/AuthSessionProvider';
import {
  fetchMyAthleteTeams,
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

/** ACTIVE athlete team memberships for consent / sharing team picker. */
export function useMyAthleteTeams() {
  const { apiClient, status } = useAuthSession();
  return useQuery({
    queryKey: organizationKeys.myAthleteTeams(),
    queryFn: () => fetchMyAthleteTeams(apiClient),
    enabled: status === 'AUTHENTICATED',
  });
}
