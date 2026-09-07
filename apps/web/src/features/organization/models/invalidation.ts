import type { QueryClient } from '@tanstack/react-query';

import { organizationKeys } from '@/features/organization/models/queryKeys';

export function invalidateInvitationQueries(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: organizationKeys.invitations() });
}

export function invalidateOrganizationQueries(queryClient: QueryClient): void {
  void queryClient.invalidateQueries({ queryKey: organizationKeys.organizations() });
  void queryClient.invalidateQueries({ queryKey: organizationKeys.teams() });
  void queryClient.invalidateQueries({ queryKey: organizationKeys.athleteTeams() });
}

/** After accept/decline/create — refresh pending invites; org/team lists may also change after accept. */
export function invalidateAfterInvitationMutation(queryClient: QueryClient): void {
  invalidateInvitationQueries(queryClient);
  invalidateOrganizationQueries(queryClient);
}
