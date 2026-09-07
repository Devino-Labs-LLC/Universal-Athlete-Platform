import { describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { InvitationsPage } from '@/features/organization/pages/InvitationsPage';
import { renderWithProviders, screen, userEvent, waitFor } from '@/test/utils';

const refetch = vi.fn();
const acceptMutateAsync = vi.fn();
const declineMutateAsync = vi.fn();

let mockInvitationsState: {
  isLoading: boolean;
  isError: boolean;
  data: unknown;
  error?: unknown;
  refetch: typeof refetch;
};

vi.mock('@/features/organization/hooks/useMyInvitations', () => ({
  useMyInvitations: () => mockInvitationsState,
}));

vi.mock('@/features/organization/hooks/useInvitationMutations', () => ({
  useAcceptInvitationByIdMutation: () => ({
    mutateAsync: acceptMutateAsync,
    isPending: false,
  }),
  useDeclineInvitationByIdMutation: () => ({
    mutateAsync: declineMutateAsync,
    isPending: false,
  }),
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: { axios: {} },
    status: 'AUTHENTICATED',
  }),
}));

describe('InvitationsPage', () => {
  it('shows loading and empty states', () => {
    mockInvitationsState = { isLoading: true, isError: false, data: undefined, refetch };
    const { unmount } = renderWithProviders(<InvitationsPage />);
    expect(screen.getByText('Loading invitations…')).toBeInTheDocument();
    unmount();

    mockInvitationsState = { isLoading: false, isError: false, data: [], refetch };
    renderWithProviders(<InvitationsPage />);
    expect(screen.getByText('No pending invitations')).toBeInTheDocument();
  });

  it('renders pending invitations without exposing a raw token', () => {
    mockInvitationsState = {
      isLoading: false,
      isError: false,
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
      refetch,
    };
    renderWithProviders(<InvitationsPage />);
    expect(screen.getByText('Devino Labs')).toBeInTheDocument();
    expect(screen.getByText(/Varsity/)).toBeInTheDocument();
    expect(screen.getByText('Athlete')).toBeInTheDocument();
    expect(screen.queryByText(/rawToken/i)).not.toBeInTheDocument();
    expect(screen.queryByText(/one-time/i)).not.toBeInTheDocument();
  });

  it('accepts and declines by invitation id', async () => {
    const user = userEvent.setup();
    acceptMutateAsync.mockResolvedValue({ organizationMembership: null, teamMembership: null });
    declineMutateAsync.mockResolvedValue(undefined);
    mockInvitationsState = {
      isLoading: false,
      isError: false,
      data: [
        {
          id: 'inv-1',
          organizationId: 'org-1',
          organizationName: 'Devino Labs',
          role: 'COACH',
          expiresAt: '2026-09-13T12:00:00Z',
        },
      ],
      refetch,
    };

    renderWithProviders(<InvitationsPage />);
    await user.click(screen.getByRole('button', { name: 'Accept' }));
    await waitFor(() => expect(acceptMutateAsync).toHaveBeenCalledWith('inv-1'));
    expect(await screen.findByText('Invitation accepted.')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Decline' }));
    await waitFor(() => expect(declineMutateAsync).toHaveBeenCalledWith('inv-1'));
    expect(await screen.findByText('Invitation declined.')).toBeInTheDocument();
  });

  it('surfaces accept errors without logging tokens', async () => {
    const user = userEvent.setup();
    acceptMutateAsync.mockRejectedValue(
      new ApiError('Invitation was not found', {
        category: 'NOT_FOUND',
        status: 404,
        code: 'INVITATION_NOT_FOUND',
      }),
    );
    mockInvitationsState = {
      isLoading: false,
      isError: false,
      data: [
        {
          id: 'inv-1',
          organizationId: 'org-1',
          organizationName: 'Devino Labs',
          role: 'ATHLETE',
          expiresAt: '2026-09-13T12:00:00Z',
        },
      ],
      refetch,
    };

    renderWithProviders(<InvitationsPage />);
    await user.click(screen.getByRole('button', { name: 'Accept' }));
    expect(
      await screen.findByText(
        'This invitation is unavailable. It may have expired, been revoked, or already been used.',
      ),
    ).toBeInTheDocument();
  });
});
