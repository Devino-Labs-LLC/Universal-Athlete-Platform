import { describe, expect, it, vi } from 'vitest';

import {
  createConsentGrant,
  fetchMyConsentGrants,
  revokeConsentGrant,
} from '@/features/consent/api/consentsApi';
import { consentGrantSchema, CONSENT_SCOPES } from '@/features/consent/models/schemas';
import { fetchMyAthleteTeams } from '@/features/organization/api/organizationsApi';

function makeClient() {
  return {
    axios: {
      get: vi.fn(),
      post: vi.fn(),
    },
  };
}

const grantPayload = {
  id: '11111111-1111-1111-1111-111111111111',
  athleteId: '22222222-2222-2222-2222-222222222222',
  teamId: '33333333-3333-3333-3333-333333333333',
  organizationId: '44444444-4444-4444-4444-444444444444',
  teamMembershipId: '55555555-5555-5555-5555-555555555555',
  scopes: ['AVAILABILITY', 'READINESS_CATEGORY'],
  status: 'ACTIVE',
  createdAt: '2026-09-07T12:00:00Z',
  revokedAt: null,
  updatedAt: '2026-09-07T12:00:00Z',
  version: 0,
};

describe('consentsApi', () => {
  it('fetches consent grants as a bare array', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({ data: [grantPayload] });

    const list = await fetchMyConsentGrants(client as never);
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/athletes/me/consents');
    expect(list).toHaveLength(1);
    expect(list[0]!.scopes).toEqual(['AVAILABILITY', 'READINESS_CATEGORY']);
    expect(list[0]!.status).toBe('ACTIVE');
  });

  it('parses create response and posts grant body', async () => {
    const client = makeClient();
    client.axios.post.mockResolvedValue({ data: grantPayload });

    const grant = await createConsentGrant(client as never, {
      teamId: '33333333-3333-3333-3333-333333333333',
      scopes: ['TRAINING_COLLABORATION'],
    });

    expect(client.axios.post).toHaveBeenCalledWith('/api/v1/athletes/me/consents', {
      teamId: '33333333-3333-3333-3333-333333333333',
      scopes: ['TRAINING_COLLABORATION'],
    });
    expect(grant.id).toBe(grantPayload.id);
  });

  it('posts revoke and returns void', async () => {
    const client = makeClient();
    client.axios.post.mockResolvedValue({ data: null });
    await revokeConsentGrant(client as never, '11111111-1111-1111-1111-111111111111');
    expect(client.axios.post).toHaveBeenCalledWith(
      '/api/v1/athletes/me/consents/11111111-1111-1111-1111-111111111111/revoke',
    );
  });

  it('rejects unknown scopes in grant payloads', () => {
    const parsed = consentGrantSchema.safeParse({
      ...grantPayload,
      scopes: ['NOT_A_REAL_SCOPE'],
    });
    expect(parsed.success).toBe(false);
  });

  it('exposes the canonical scope set without share-all', () => {
    expect(CONSENT_SCOPES).toEqual([
      'AVAILABILITY',
      'READINESS_CATEGORY',
      'READINESS_SCORE',
      'LIMITING_DIMENSIONS',
      'RECOVERY_CHECK_IN_DETAIL',
      'TRAINING_ADHERENCE',
      'PERFORMANCE_HISTORY',
      'TRAINING_COLLABORATION',
      'EXPORT',
    ]);
  });
});

describe('athlete teams api for consent picker', () => {
  it('fetches ACTIVE athlete team memberships with names', async () => {
    const client = makeClient();
    client.axios.get.mockResolvedValue({
      data: [
        {
          membershipId: 'mem-1',
          teamId: 'team-1',
          teamName: 'Varsity',
          organizationId: 'org-1',
          organizationName: 'Devino Labs',
          athleteId: 'ath-1',
        },
      ],
    });

    const teams = await fetchMyAthleteTeams(client as never);
    expect(client.axios.get).toHaveBeenCalledWith('/api/v1/athletes/me/teams');
    expect(teams[0]!.teamName).toBe('Varsity');
    expect(teams[0]!.organizationName).toBe('Devino Labs');
  });
});
