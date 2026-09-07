import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';

import {
  invalidateAfterInvitationMutation,
  invalidateInvitationQueries,
  invalidateOrganizationQueries,
} from '@/features/organization/models/invalidation';
import { organizationKeys } from '@/features/organization/models/queryKeys';

describe('organization invalidation', () => {
  it('invalidates invitation queries', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateInvitationQueries(client);
    expect(spy).toHaveBeenCalledWith({ queryKey: organizationKeys.invitations() });
  });

  it('invalidates organization and team list queries', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateOrganizationQueries(client);
    expect(spy).toHaveBeenCalledWith({ queryKey: organizationKeys.organizations() });
    expect(spy).toHaveBeenCalledWith({ queryKey: organizationKeys.teams() });
    expect(spy).toHaveBeenCalledWith({ queryKey: organizationKeys.athleteTeams() });
  });

  it('invalidates invitations plus org/team lists after invitation mutations', () => {
    const client = new QueryClient();
    const spy = vi.spyOn(client, 'invalidateQueries');
    invalidateAfterInvitationMutation(client);
    expect(spy).toHaveBeenCalledWith({ queryKey: organizationKeys.invitations() });
    expect(spy).toHaveBeenCalledWith({ queryKey: organizationKeys.organizations() });
    expect(spy).toHaveBeenCalledWith({ queryKey: organizationKeys.teams() });
    expect(spy).toHaveBeenCalledWith({ queryKey: organizationKeys.athleteTeams() });
  });
});
