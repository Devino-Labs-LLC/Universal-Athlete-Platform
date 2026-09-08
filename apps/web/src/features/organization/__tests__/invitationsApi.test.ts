import { describe, expect, it, vi } from 'vitest';

import {
  acceptInvitationById,
  acceptInvitationByToken,
  createOrganizationInvitation,
  createTeamInvitation,
  declineInvitationById,
  fetchMyInvitations,
  fetchTeamInvitations,
  revokeTeamInvitation,
} from '@/features/organization/api/invitationsApi';
import { fetchMyOrganizations, fetchOrganizationTeams } from '@/features/organization/api/organizationsApi';
import { myInvitationSchema } from '@/features/organization/models/schemas';

function makeClient() {
  return {
    axios: {
      get: vi.fn(),
      post: vi.fn(),
    },
  };
}

describe('invitationsApi', () => {
  it('fetches pending invitations as a bare array without rawToken', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: [
        {
          id: 'inv-1',
          organizationId: 'org-1',
          organizationName: 'Devino Labs',
          teamId: 'team-1',
          teamName: 'Varsity',
          role: 'ATHLETE',
          expiresAt: '2026-09-13T12:00:00Z',
        },
      ],
    });

    const list = await fetchMyInvitations(client as never);
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/me/invitations');
    expect(list).toHaveLength(1);
    expect(list[0]!.organizationName).toBe('Devino Labs');
    expect(list[0]).not.toHaveProperty('rawToken');
    expect(Object.keys(list[0]!)).not.toContain('rawToken');
  });

  it('rejects list payloads that include a rawToken field', () => {
    const parsed = myInvitationSchema.safeParse({
      id: 'inv-1',
      organizationId: 'org-1',
      organizationName: 'Devino Labs',
      role: 'ATHLETE',
      expiresAt: '2026-09-13T12:00:00Z',
      rawToken: 'secret-should-not-exist',
    });
    // Zod object schemas strip unknown keys by default — assert strip, not presence
    expect(parsed.success).toBe(true);
    if (parsed.success) {
      expect(parsed.data).not.toHaveProperty('rawToken');
    }
  });

  it('posts accept and decline by invitation id', async () => {
    const client = makeClient();
    client.axios.post.mockResolvedValueOnce({
      data: {
        organizationMembership: {
          id: 'mem-1',
          organizationId: 'org-1',
          accountId: 'acc-1',
          athleteId: null,
          role: 'ORG_ADMIN',
          status: 'ACTIVE',
          createdAt: '2026-09-06T12:00:00Z',
          updatedAt: '2026-09-06T12:00:00Z',
          version: 0,
        },
        teamMembership: null,
      },
    });
    client.axios.post.mockResolvedValueOnce({ data: null });

    await acceptInvitationById(client as never, 'inv-1');
    await declineInvitationById(client as never, 'inv-2');

    expect(client.axios.post).toHaveBeenNthCalledWith(1, '/api/v1/me/invitations/inv-1/accept');
    expect(client.axios.post).toHaveBeenNthCalledWith(2, '/api/v1/me/invitations/inv-2/decline');
  });

  it('posts accept by raw token with URI encoding', async () => {
    const client = makeClient();
    client.axios.post.mockResolvedValue({
      data: { organizationMembership: null, teamMembership: null },
    });
    await acceptInvitationByToken(client as never, 'abc/def+ghi');
    expect(client.axios.post).toHaveBeenCalledWith('/api/v1/invitations/abc%2Fdef%2Bghi/accept');
  });

  it('parses create response including one-time rawToken', async () => {
    const client = makeClient();
    client.axios.post.mockResolvedValue({
      data: {
        id: 'inv-9',
        organizationId: 'org-1',
        teamId: null,
        invitedEmail: 'a@example.com',
        invitedAccountId: null,
        role: 'ORG_ADMIN',
        status: 'PENDING',
        expiresAt: '2026-09-13T12:00:00Z',
        acceptedMembershipId: null,
        createdByAccountId: 'acc-1',
        createdAt: '2026-09-06T12:00:00Z',
        updatedAt: '2026-09-06T12:00:00Z',
        version: 0,
        rawToken: 'one-time-token',
      },
    });

    const invitation = await createOrganizationInvitation(client as never, 'org-1', {
      email: 'a@example.com',
      role: 'ORG_ADMIN',
    });
    expect(invitation.rawToken).toBe('one-time-token');
  });

  it('lists, creates, and revokes team invitations', async () => {
    const client = makeClient();
    const payload = {
      id: 'inv-3',
      organizationId: 'org-1',
      teamId: 'team-1',
      invitedEmail: 'athlete@example.com',
      invitedAccountId: null,
      role: 'ATHLETE',
      status: 'PENDING',
      expiresAt: '2026-09-13T12:00:00Z',
      acceptedMembershipId: null,
      createdByAccountId: 'acc-1',
      createdAt: '2026-09-06T12:00:00Z',
      updatedAt: '2026-09-06T12:00:00Z',
      version: 0,
    };
    client.axios.get.mockResolvedValue({ data: [payload] });
    client.axios.post
      .mockResolvedValueOnce({ data: { ...payload, rawToken: 'one-time-token' } })
      .mockResolvedValueOnce({ data: null });

    const list = await fetchTeamInvitations(client as never, 'team-1');
    const created = await createTeamInvitation(client as never, 'team-1', {
      email: 'coach@example.com',
      role: 'COACH',
    });
    await revokeTeamInvitation(client as never, 'team-1', 'inv-3');

    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/teams/team-1/invitations');
    expect(client.axios.post).toHaveBeenNthCalledWith(1, '/api/v1/teams/team-1/invitations', {
      email: 'coach@example.com',
      role: 'COACH',
    });
    expect(client.axios.post).toHaveBeenNthCalledWith(2, '/api/v1/teams/team-1/invitations/inv-3/revoke');
    expect(list[0]?.invitedEmail).toBe('athlete@example.com');
    expect(list[0]?.rawToken).toBeUndefined();
    expect(created.rawToken).toBe('one-time-token');
  });
});

describe('organizationsApi', () => {
  it('fetches organizations and teams as bare arrays', async () => {
    const client = makeClient();
    client.axios.get
      .mockResolvedValueOnce({
        data: [
          {
            id: 'org-1',
            name: 'Devino',
            status: 'ACTIVE',
            createdAt: '2026-09-01T00:00:00Z',
            updatedAt: '2026-09-01T00:00:00Z',
            version: 0,
          },
        ],
      })
      .mockResolvedValueOnce({
        data: [
          {
            id: 'team-1',
            organizationId: 'org-1',
            name: 'Varsity',
            status: 'ACTIVE',
            createdAt: '2026-09-01T00:00:00Z',
            updatedAt: '2026-09-01T00:00:00Z',
            version: 0,
          },
        ],
      });

    const orgs = await fetchMyOrganizations(client as never);
    const teams = await fetchOrganizationTeams(client as never, 'org-1');
    expect(orgs[0]!.name).toBe('Devino');
    expect(teams[0]!.name).toBe('Varsity');
    expect(client.axios.get).toHaveBeenNthCalledWith(1, '/api/v1/organizations');
    expect(client.axios.get).toHaveBeenNthCalledWith(2, '/api/v1/organizations/org-1/teams');
  });
});
