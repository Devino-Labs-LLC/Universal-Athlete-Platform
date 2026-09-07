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
});
