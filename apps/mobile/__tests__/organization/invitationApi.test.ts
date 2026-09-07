import {
  acceptInvitationById,
  acceptInvitationByToken,
  declineInvitationById,
  declineInvitationByToken,
  listMyInvitations,
} from '@/src/features/organization/api/invitationsApi';
import {
  acceptInvitationResponseSchema,
  myInvitationSchema,
} from '@/src/features/organization/models/invitationSchemas';
import {
  formatInvitationExpiry,
  invitationRoleLabel,
  invitationScopeSubtitle,
  invitationScopeTitle,
} from '@/src/features/organization/models/invitationLabels';
import {
  invitationErrorMessage,
  invitationErrorTitle,
  isInvitationUnavailableError,
} from '@/src/features/organization/utils/invitationErrors';
import { ApiError } from '@/src/core/api/errors';

describe('invitationSchemas', () => {
  it('parses MyInvitationResponse fields', () => {
    const parsed = myInvitationSchema.parse({
      id: 'inv-1',
      organizationId: 'org-1',
      organizationName: 'Org',
      teamId: null,
      teamName: null,
      role: 'ATHLETE',
      expiresAt: '2026-09-13T12:00:00Z',
    });
    expect(parsed.organizationName).toBe('Org');
    expect(parsed.teamId).toBeNull();
  });

  it('parses AcceptInvitationResponse with nullable memberships', () => {
    const parsed = acceptInvitationResponseSchema.parse({
      organizationMembership: {
        id: 'om-1',
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
    });
    expect(parsed.organizationMembership?.role).toBe('ORG_ADMIN');
    expect(parsed.teamMembership).toBeNull();
  });
});

describe('invitationsApi', () => {
  it('calls list and id accept/decline endpoints', async () => {
    const get = jest.fn().mockResolvedValue({
      data: [
        {
          id: 'inv-1',
          organizationId: 'org-1',
          organizationName: 'Org',
          teamId: 'team-1',
          teamName: 'Team',
          role: 'ATHLETE',
          expiresAt: '2026-09-13T12:00:00Z',
        },
      ],
    });
    const post = jest.fn().mockResolvedValue({
      data: {
        organizationMembership: null,
        teamMembership: {
          id: 'tm-1',
          teamId: 'team-1',
          accountId: 'acc-1',
          athleteId: 'ath-1',
          role: 'ATHLETE',
          status: 'ACTIVE',
          createdAt: '2026-09-06T12:00:00Z',
          updatedAt: '2026-09-06T12:00:00Z',
          version: 0,
        },
      },
    });
    const client = { axios: { get, post } };

    await listMyInvitations(client as never);
    await acceptInvitationById(client as never, 'inv-1');
    await declineInvitationById(client as never, 'inv-1');

    expect(get).toHaveBeenCalledWith('/api/v1/me/invitations');
    expect(post).toHaveBeenCalledWith('/api/v1/me/invitations/inv-1/accept');
    expect(post).toHaveBeenCalledWith('/api/v1/me/invitations/inv-1/decline');
  });

  it('encodes raw token in path and does not require logging helpers', async () => {
    const post = jest.fn().mockResolvedValue({
      data: { organizationMembership: null, teamMembership: null },
    });
    const client = { axios: { post } };
    const token = 'abc+/=def';

    await acceptInvitationByToken(client as never, token);
    await declineInvitationByToken(client as never, token);

    expect(post).toHaveBeenCalledWith(
      `/api/v1/invitations/${encodeURIComponent(token)}/accept`,
    );
    expect(post).toHaveBeenCalledWith(
      `/api/v1/invitations/${encodeURIComponent(token)}/decline`,
    );
  });
});

describe('invitationErrors', () => {
  it('maps not-found as unavailable without leaking codes into title only', () => {
    const error = new ApiError('Invitation was not found', {
      category: 'notFound',
      code: 'INVITATION_NOT_FOUND',
    });
    expect(isInvitationUnavailableError(error)).toBe(true);
    expect(invitationErrorTitle(error)).toBe('Invitation unavailable');
    expect(invitationErrorMessage(error)).toMatch(/expired or been revoked/i);
  });

  it('maps known invitation conflict codes to friendly copy', () => {
    expect(
      invitationErrorMessage(
        new ApiError('x', { category: 'conflict', code: 'EMAIL_UNVERIFIED' }),
      ),
    ).toMatch(/Verify your email/i);
    expect(
      invitationErrorTitle(
        new ApiError('x', { category: 'conflict', code: 'EMAIL_UNVERIFIED' }),
      ),
    ).toBe('Email verification required');

    expect(
      invitationErrorMessage(
        new ApiError('x', { category: 'conflict', code: 'ATHLETE_PROFILE_REQUIRED' }),
      ),
    ).toMatch(/Complete your athlete profile/i);
    expect(
      invitationErrorTitle(
        new ApiError('x', { category: 'conflict', code: 'ATHLETE_PROFILE_REQUIRED' }),
      ),
    ).toBe('Profile required');

    expect(
      invitationErrorMessage(
        new ApiError('x', { category: 'conflict', code: 'MEMBERSHIP_ALREADY_ACTIVE' }),
      ),
    ).toMatch(/already have an active membership/i);
    expect(
      invitationErrorTitle(
        new ApiError('x', { category: 'conflict', code: 'MEMBERSHIP_ALREADY_ACTIVE' }),
      ),
    ).toBe('Already a member');

    expect(
      invitationErrorMessage(
        new ApiError('x', { category: 'conflict', code: 'OPTIMISTIC_LOCK_CONFLICT' }),
      ),
    ).toMatch(/try again/i);
  });

  it('falls back for unknown ApiError codes, Error instances, and unknown values', () => {
    expect(
      invitationErrorMessage(
        new ApiError('Server said no', { category: 'server', code: 'OTHER' }),
      ),
    ).toBe('Server said no');
    expect(invitationErrorTitle(new ApiError('x', { category: 'server' }))).toBe(
      'Something went wrong',
    );
    expect(invitationErrorMessage(new Error('boom'))).toBe('boom');
    expect(invitationErrorMessage('plain')).toBe('Something went wrong. Please try again.');
    expect(isInvitationUnavailableError(new Error('nope'))).toBe(false);
  });
});

describe('invitationLabels', () => {
  it('formats role and expiry values', () => {
    expect(invitationRoleLabel('ATHLETE')).toBe('Athlete');
    expect(formatInvitationExpiry('not-a-date')).toBe('not-a-date');
    expect(formatInvitationExpiry('2026-09-13T12:00:00Z')).toMatch(/2026/);
  });

  it('builds scope title and subtitle from team vs organization', () => {
    expect(
      invitationScopeTitle({
        organizationName: 'Org',
        teamName: 'Team A',
      }),
    ).toBe('Team A');
    expect(
      invitationScopeSubtitle({
        organizationName: 'Org',
        teamName: 'Team A',
      }),
    ).toBe('Org');
    expect(
      invitationScopeTitle({
        organizationName: 'Org Only',
        teamName: null,
      }),
    ).toBe('Org Only');
    expect(
      invitationScopeSubtitle({
        organizationName: 'Org Only',
        teamName: null,
      }),
    ).toBeUndefined();
  });
});
