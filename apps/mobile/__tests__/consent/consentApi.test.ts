import {
  createConsentGrant,
  listMyAthleteTeamMemberships,
  listMyConsents,
  revokeConsentGrant,
} from '@/src/features/consent/api/consentsApi';
import {
  consentScopeDescription,
  consentScopeLabel,
  consentStatusLabel,
  formatConsentScopesSummary,
  resolveTeamLabel,
} from '@/src/features/consent/models/consentLabels';
import {
  CONSENT_SCOPES,
  consentGrantSchema,
  createConsentGrantRequestSchema,
  myAthleteTeamMembershipSchema,
} from '@/src/features/consent/models/consentSchemas';
import {
  consentErrorMessage,
  consentErrorTitle,
} from '@/src/features/consent/utils/consentErrors';
import { ApiError } from '@/src/core/api/errors';

const grantFixture = {
  id: 'cg-1',
  athleteId: 'ath-1',
  teamId: 'team-1',
  organizationId: 'org-1',
  teamMembershipId: 'tm-1',
  scopes: ['AVAILABILITY', 'READINESS_CATEGORY'],
  status: 'ACTIVE',
  createdAt: '2026-09-06T12:00:00Z',
  revokedAt: null,
  updatedAt: '2026-09-06T12:00:00Z',
  version: 0,
};

describe('consentSchemas', () => {
  it('parses ConsentGrantResponse fields', () => {
    const parsed = consentGrantSchema.parse(grantFixture);
    expect(parsed.status).toBe('ACTIVE');
    expect(parsed.scopes).toEqual(['AVAILABILITY', 'READINESS_CATEGORY']);
  });

  it('requires at least one scope on create request', () => {
    expect(() =>
      createConsentGrantRequestSchema.parse({ teamId: '00000000-0000-4000-8000-000000000001', scopes: [] }),
    ).toThrow();
    expect(
      createConsentGrantRequestSchema.parse({
        teamId: '00000000-0000-4000-8000-000000000001',
        scopes: ['EXPORT'],
      }).scopes,
    ).toEqual(['EXPORT']);
  });

  it('parses athlete team membership picker rows', () => {
    const parsed = myAthleteTeamMembershipSchema.parse({
      membershipId: 'tm-1',
      teamId: 'team-1',
      teamName: 'Varsity',
      organizationId: 'org-1',
      organizationName: 'Devino Labs',
      athleteId: 'ath-1',
    });
    expect(parsed.teamName).toBe('Varsity');
  });

  it('exposes all canonical scopes with none implied as default', () => {
    expect(CONSENT_SCOPES).toHaveLength(9);
    expect(CONSENT_SCOPES).toContain('TRAINING_COLLABORATION');
  });
});

describe('consentsApi', () => {
  it('calls list, create, revoke, and athlete teams endpoints', async () => {
    const get = jest
      .fn()
      .mockResolvedValueOnce({ data: [grantFixture] })
      .mockResolvedValueOnce({
        data: [
          {
            membershipId: 'tm-1',
            teamId: 'team-1',
            teamName: 'Varsity',
            organizationId: 'org-1',
            organizationName: 'Devino Labs',
            athleteId: 'ath-1',
          },
        ],
      });
    const post = jest
      .fn()
      .mockResolvedValueOnce({ data: grantFixture })
      .mockResolvedValueOnce({ data: undefined });
    const client = { axios: { get, post } };

    await listMyConsents(client as never);
    await createConsentGrant(client as never, {
      teamId: '00000000-0000-4000-8000-000000000001',
      scopes: ['AVAILABILITY'],
    });
    await revokeConsentGrant(client as never, 'cg-1');
    await listMyAthleteTeamMemberships(client as never);

    expect(get).toHaveBeenCalledWith('/api/v1/athletes/me/consents');
    expect(post).toHaveBeenCalledWith('/api/v1/athletes/me/consents', {
      teamId: '00000000-0000-4000-8000-000000000001',
      scopes: ['AVAILABILITY'],
    });
    expect(post).toHaveBeenCalledWith('/api/v1/athletes/me/consents/cg-1/revoke');
    expect(get).toHaveBeenCalledWith('/api/v1/athletes/me/teams');
  });
});

describe('consentErrors', () => {
  it('maps known consent conflict codes to friendly copy', () => {
    expect(
      consentErrorMessage(
        new ApiError('x', { category: 'conflict', code: 'ACTIVE_GRANT_EXISTS' }),
      ),
    ).toMatch(/already have an active sharing grant/i);
    expect(
      consentErrorTitle(
        new ApiError('x', { category: 'conflict', code: 'ACTIVE_GRANT_EXISTS' }),
      ),
    ).toBe('Already sharing');

    expect(
      consentErrorMessage(
        new ApiError('x', { category: 'notFound', code: 'CONSENT_NOT_FOUND' }),
      ),
    ).toMatch(/not found|no longer available/i);
    expect(
      consentErrorTitle(new ApiError('x', { category: 'notFound', code: 'CONSENT_NOT_FOUND' })),
    ).toBe('Sharing unavailable');

    expect(
      consentErrorTitle(new ApiError('x', { category: 'conflict', code: 'TEAM_ARCHIVED' })),
    ).toBe('Team unavailable');
  });

  it('falls back for unknown ApiError codes, Error instances, and unknown values', () => {
    expect(
      consentErrorMessage(new ApiError('Server said no', { category: 'server', code: 'OTHER' })),
    ).toBe('Server said no');
    expect(consentErrorTitle(new ApiError('x', { category: 'server' }))).toBe(
      'Something went wrong',
    );
    expect(consentErrorMessage(new Error('boom'))).toBe('boom');
    expect(consentErrorMessage('plain')).toBe('Something went wrong. Please try again.');
  });
});

describe('consentLabels', () => {
  it('formats scope and status labels', () => {
    expect(consentScopeLabel('READINESS_SCORE')).toBe('Readiness score');
    expect(consentScopeDescription('EXPORT')).toMatch(/bulk export/i);
    expect(consentStatusLabel('ACTIVE')).toBe('Sharing');
    expect(consentStatusLabel('REVOKED')).toBe('Revoked');
    expect(formatConsentScopesSummary([])).toBe('No scopes');
    expect(formatConsentScopesSummary(['AVAILABILITY'])).toBe('Availability');
    expect(formatConsentScopesSummary(['AVAILABILITY', 'EXPORT'])).toBe('2 scopes');
  });

  it('resolves team labels from memberships when names are missing on grants', () => {
    expect(
      resolveTeamLabel(
        { teamId: 'team-1' },
        [
          {
            membershipId: 'tm-1',
            teamId: 'team-1',
            teamName: 'Varsity',
            organizationId: 'org-1',
            organizationName: 'Devino Labs',
            athleteId: 'ath-1',
          },
        ],
      ),
    ).toEqual({ title: 'Varsity', subtitle: 'Devino Labs' });
    expect(resolveTeamLabel({ teamId: 'missing' }, [])).toEqual({
      title: 'Team',
      subtitle: undefined,
    });
  });
});
