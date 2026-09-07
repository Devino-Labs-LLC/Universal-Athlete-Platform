import { describe, expect, it } from 'vitest';

import { organizationKeys } from '@/features/organization/models/queryKeys';

describe('organizationKeys', () => {
  it('nests organizations, teams, and invitations under a shared root', () => {
    expect(organizationKeys.all).toEqual(['organization']);
    expect(organizationKeys.organizationList()).toEqual(['organization', 'organizations', 'list']);
    expect(organizationKeys.teamList('org-1')).toEqual(['organization', 'teams', 'list', 'org-1']);
    expect(organizationKeys.myInvitations()).toEqual(['organization', 'invitations', 'mine']);
    expect(organizationKeys.myAthleteTeams()).toEqual(['organization', 'athleteTeams', 'mine']);
  });

  it('scopes team lists by organization id', () => {
    expect(organizationKeys.teamList('org-a')).not.toEqual(organizationKeys.teamList('org-b'));
  });
});
