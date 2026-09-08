import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { TeamInvitationsPage } from '@/features/coach/pages/TeamInvitationsPage';
import { renderWithProviders, screen, userEvent, waitFor } from '@/test/utils';

const fetchTeamInvitations = vi.fn();
const createTeamInvitation = vi.fn();
const revokeTeamInvitation = vi.fn();

let teamId: string | undefined = 'team-1';

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: { axios: {} },
    account: { accountId: 'acc-1' },
  }),
}));

vi.mock('@/features/organization/api/invitationsApi', () => ({
  fetchTeamInvitations: (...args: unknown[]) => fetchTeamInvitations(...args),
  createTeamInvitation: (...args: unknown[]) => createTeamInvitation(...args),
  revokeTeamInvitation: (...args: unknown[]) => revokeTeamInvitation(...args),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useParams: () => ({ teamId }),
  };
});

const pending = {
  id: 'inv-1',
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

describe('TeamInvitationsPage', () => {
  beforeEach(() => {
    teamId = 'team-1';
    fetchTeamInvitations.mockReset();
    createTeamInvitation.mockReset();
    revokeTeamInvitation.mockReset();
  });

  it('requires a team route', () => {
    teamId = undefined;
    renderWithProviders(<TeamInvitationsPage />);
    expect(screen.getByText('Team was not specified.')).toBeInTheDocument();
  });

  it('shows loading and then an empty pending list', async () => {
    fetchTeamInvitations.mockReturnValue(new Promise(() => undefined));
    const { unmount } = renderWithProviders(<TeamInvitationsPage />);
    expect(screen.getByText('Loading invitations…')).toBeInTheDocument();
    unmount();

    fetchTeamInvitations.mockResolvedValue([{ ...pending, status: 'ACCEPTED' }]);
    renderWithProviders(<TeamInvitationsPage />);
    expect(await screen.findByText('No pending invitations')).toBeInTheDocument();
  });

  it('creates an invitation and shows the one-time link without a raw token field', async () => {
    fetchTeamInvitations.mockResolvedValue([]);
    createTeamInvitation.mockResolvedValue({ ...pending, rawToken: 'one-time' });
    const user = userEvent.setup();
    renderWithProviders(<TeamInvitationsPage />);

    await user.type(screen.getByLabelText('Email'), 'athlete@example.com');
    await user.click(screen.getByRole('button', { name: 'Send invitation' }));

    await waitFor(() => {
      expect(createTeamInvitation).toHaveBeenCalled();
    });
    expect(await screen.findByText(/one-time link/i)).toHaveTextContent('/app/invitations/token/one-time');
  });

  it('revokes a pending invitation', async () => {
    fetchTeamInvitations.mockResolvedValue([pending]);
    revokeTeamInvitation.mockResolvedValue(undefined);
    const user = userEvent.setup();
    renderWithProviders(<TeamInvitationsPage />);

    await user.click(await screen.findByRole('button', { name: 'Revoke' }));
    await waitFor(() => {
      expect(revokeTeamInvitation).toHaveBeenCalledWith(expect.anything(), 'team-1', 'inv-1');
    });
  });

  it('treats an inaccessible team as unavailable and retries other failures', async () => {
    fetchTeamInvitations.mockRejectedValue(new ApiError('missing', { category: 'NOT_FOUND', status: 404 }));
    const { unmount } = renderWithProviders(<TeamInvitationsPage />);
    expect(await screen.findByText('Team unavailable')).toBeInTheDocument();
    unmount();

    fetchTeamInvitations.mockRejectedValue(new ApiError('boom', { category: 'SERVER', status: 500 }));
    const user = userEvent.setup();
    renderWithProviders(<TeamInvitationsPage />);
    expect(await screen.findByText('boom')).toBeInTheDocument();
    await user.click(screen.getByRole('button', { name: 'Try again' }));
    await waitFor(() => {
      expect(fetchTeamInvitations.mock.calls.length).toBeGreaterThan(1);
    });
  });

  it('shows a validation error when create fails', async () => {
    fetchTeamInvitations.mockResolvedValue([]);
    createTeamInvitation.mockRejectedValue(new Error('Unable to create invitation.'));
    const user = userEvent.setup();
    renderWithProviders(<TeamInvitationsPage />);

    await user.type(screen.getByLabelText('Email'), 'athlete@example.com');
    await user.click(screen.getByRole('button', { name: 'Send invitation' }));
    expect(await screen.findByRole('alert')).toHaveTextContent('Unable to create invitation.');
  });
});
