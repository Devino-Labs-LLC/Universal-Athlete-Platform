import { Route, Routes } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { InvitationTokenPage } from '@/features/organization/pages/InvitationTokenPage';
import { renderWithProviders, screen, userEvent, waitFor } from '@/test/utils';

const acceptMutateAsync = vi.fn();
const declineMutateAsync = vi.fn();

let mockInvitationsState: {
  isLoading: boolean;
  isError: boolean;
  data: unknown;
  error?: unknown;
  refetch: () => void;
};

vi.mock('@/features/organization/hooks/useMyInvitations', () => ({
  useMyInvitations: () => mockInvitationsState,
}));

vi.mock('@/features/organization/hooks/useInvitationMutations', () => ({
  useAcceptInvitationByTokenMutation: () => ({
    mutateAsync: acceptMutateAsync,
    isPending: false,
  }),
  useDeclineInvitationByTokenMutation: () => ({
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

function renderTokenPage(token: string) {
  return renderWithProviders(
    <Routes>
      <Route path="/app/invitations/token/:token" element={<InvitationTokenPage />} />
    </Routes>,
    { initialEntries: [`/app/invitations/token/${encodeURIComponent(token)}`] },
  );
}

describe('InvitationTokenPage', () => {
  it('shows generic accept/decline when list cannot resolve a single invite', () => {
    mockInvitationsState = {
      isLoading: false,
      isError: false,
      data: [],
      refetch: vi.fn(),
    };
    renderTokenPage('raw-token-value');
    expect(screen.getByText(/You have been invited/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Accept invitation' })).toBeInTheDocument();
    expect(screen.queryByText('raw-token-value')).not.toBeInTheDocument();
  });

  it('shows org/team/role when the pending list resolves a single invitation', () => {
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
      refetch: vi.fn(),
    };
    renderTokenPage('raw-token-value');
    expect(screen.getByText('Devino Labs')).toBeInTheDocument();
    expect(screen.getByText(/Varsity/)).toBeInTheDocument();
    expect(screen.getByText('Athlete')).toBeInTheDocument();
  });

  it('treats already-accepted retry as success', async () => {
    const user = userEvent.setup();
    acceptMutateAsync.mockResolvedValue({
      organizationMembership: {
        id: 'mem-1',
        organizationId: 'org-1',
        accountId: 'acc-1',
        role: 'ATHLETE',
        status: 'ACTIVE',
        createdAt: '2026-09-06T12:00:00Z',
        updatedAt: '2026-09-06T12:00:00Z',
        version: 0,
      },
      teamMembership: null,
    });
    mockInvitationsState = {
      isLoading: false,
      isError: false,
      data: [],
      refetch: vi.fn(),
    };

    renderTokenPage('raw-token-value');
    await user.click(screen.getByRole('button', { name: 'Accept invitation' }));
    await waitFor(() => expect(acceptMutateAsync).toHaveBeenCalledWith('raw-token-value'));
    expect(await screen.findByRole('heading', { name: 'Invitation accepted' })).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(/Invitation accepted/i);
  });

  it('maps expired/revoked/not-found to an unavailable state', async () => {
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
      data: [],
      refetch: vi.fn(),
    };

    renderTokenPage('expired-token');
    await user.click(screen.getByRole('button', { name: 'Accept invitation' }));
    expect(await screen.findByText('Invitation unavailable')).toBeInTheDocument();
    expect(
      screen.getByText(
        'This invitation is unavailable. It may have expired, been revoked, or already been used.',
      ),
    ).toBeInTheDocument();
  });
});
