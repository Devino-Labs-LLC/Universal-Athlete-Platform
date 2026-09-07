import { describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { CONSENT_SCOPE_CATALOG } from '@/features/consent/models/scopes';
import { SharingPage } from '@/features/consent/pages/SharingPage';
import { renderWithProviders, screen, userEvent, waitFor, within } from '@/test/utils';

const teamsRefetch = vi.fn();
const grantsRefetch = vi.fn();
const createMutateAsync = vi.fn();
const revokeMutateAsync = vi.fn();

let mockTeamsState: {
  isLoading: boolean;
  isError: boolean;
  data: unknown;
  error?: unknown;
  refetch: typeof teamsRefetch;
};

let mockGrantsState: {
  isLoading: boolean;
  isError: boolean;
  data: unknown;
  error?: unknown;
  refetch: typeof grantsRefetch;
};

vi.mock('@/features/organization/hooks/useOrganizations', () => ({
  useMyAthleteTeams: () => mockTeamsState,
}));

vi.mock('@/features/consent/hooks/useMyConsentGrants', () => ({
  useMyConsentGrants: () => mockGrantsState,
}));

vi.mock('@/features/consent/hooks/useConsentMutations', () => ({
  useCreateConsentGrantMutation: () => ({
    mutateAsync: createMutateAsync,
    isPending: false,
  }),
  useRevokeConsentGrantMutation: () => ({
    mutateAsync: revokeMutateAsync,
    isPending: false,
  }),
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: { axios: {} },
    status: 'AUTHENTICATED',
  }),
}));

const teamFixture = {
  membershipId: 'mem-1',
  teamId: '33333333-3333-3333-3333-333333333333',
  teamName: 'Varsity',
  organizationId: '44444444-4444-4444-4444-444444444444',
  organizationName: 'Devino Labs',
  athleteId: 'ath-1',
};

const activeGrant = {
  id: 'grant-active',
  athleteId: 'ath-1',
  teamId: '33333333-3333-3333-3333-333333333333',
  organizationId: '44444444-4444-4444-4444-444444444444',
  teamMembershipId: 'mem-1',
  scopes: ['AVAILABILITY'],
  status: 'ACTIVE',
  createdAt: '2026-09-01T12:00:00Z',
  revokedAt: null,
  updatedAt: '2026-09-01T12:00:00Z',
  version: 0,
};

const revokedGrant = {
  ...activeGrant,
  id: 'grant-revoked',
  teamId: '66666666-6666-6666-6666-666666666666',
  teamMembershipId: 'mem-2',
  scopes: ['READINESS_SCORE'],
  status: 'REVOKED',
  revokedAt: '2026-09-02T12:00:00Z',
};

describe('SharingPage', () => {
  it('shows loading and join-team empty state', () => {
    mockTeamsState = { isLoading: true, isError: false, data: undefined, refetch: teamsRefetch };
    mockGrantsState = { isLoading: true, isError: false, data: undefined, refetch: grantsRefetch };
    const { unmount } = renderWithProviders(<SharingPage />);
    expect(screen.getByText('Loading sharing controls…')).toBeInTheDocument();
    unmount();

    mockTeamsState = { isLoading: false, isError: false, data: [], refetch: teamsRefetch };
    mockGrantsState = { isLoading: false, isError: false, data: [], refetch: grantsRefetch };
    renderWithProviders(<SharingPage />);
    expect(screen.getByText('Join a team first')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: 'View invitations' })).toHaveAttribute(
      'href',
      '/app/invitations',
    );
  });

  it('does not preselect any scopes and requires an explicit selection', async () => {
    const user = userEvent.setup();
    mockTeamsState = {
      isLoading: false,
      isError: false,
      data: [teamFixture],
      refetch: teamsRefetch,
    };
    mockGrantsState = { isLoading: false, isError: false, data: [], refetch: grantsRefetch };

    renderWithProviders(<SharingPage />);

    for (const entry of CONSENT_SCOPE_CATALOG) {
      expect(screen.getByRole('checkbox', { name: new RegExp(entry.label, 'i') })).not.toBeChecked();
    }

    await user.selectOptions(screen.getByLabelText('Team'), teamFixture.teamId);
    await user.click(screen.getByRole('button', { name: 'Grant selected access' }));
    expect(
      await screen.findByText(/select at least one sharing scope/i),
    ).toBeInTheDocument();
    expect(createMutateAsync).not.toHaveBeenCalled();
  });

  it('grants selected scopes for a team', async () => {
    const user = userEvent.setup();
    createMutateAsync.mockResolvedValue({ ...activeGrant, scopes: ['READINESS_CATEGORY'] });
    mockTeamsState = {
      isLoading: false,
      isError: false,
      data: [teamFixture],
      refetch: teamsRefetch,
    };
    mockGrantsState = { isLoading: false, isError: false, data: [], refetch: grantsRefetch };

    renderWithProviders(<SharingPage />);
    await user.selectOptions(screen.getByLabelText('Team'), teamFixture.teamId);
    await user.click(screen.getByRole('checkbox', { name: /readiness category/i }));
    await user.click(screen.getByRole('button', { name: 'Grant selected access' }));

    await waitFor(() =>
      expect(createMutateAsync).toHaveBeenCalledWith({
        teamId: teamFixture.teamId,
        scopes: ['READINESS_CATEGORY'],
      }),
    );
    expect(await screen.findByText(/sharing grant created/i)).toBeInTheDocument();
  });

  it('lists active and revoked grants and revokes after confirmation', async () => {
    const user = userEvent.setup();
    revokeMutateAsync.mockResolvedValue(undefined);
    mockTeamsState = {
      isLoading: false,
      isError: false,
      data: [teamFixture],
      refetch: teamsRefetch,
    };
    mockGrantsState = {
      isLoading: false,
      isError: false,
      data: [activeGrant, revokedGrant],
      refetch: grantsRefetch,
    };

    renderWithProviders(<SharingPage />);
    expect(screen.getByText('Varsity')).toBeInTheDocument();
    expect(screen.getByText(/Devino Labs/)).toBeInTheDocument();
    expect(screen.getByText('Active')).toBeInTheDocument();
    expect(screen.getByText('Revoked')).toBeInTheDocument();

    await user.click(screen.getByRole('button', { name: 'Revoke access' }));
    const dialog = screen.getByRole('alertdialog');
    expect(within(dialog).getByText(/immediately lose access/i)).toBeInTheDocument();
    await user.click(within(dialog).getByRole('button', { name: 'Revoke access' }));

    await waitFor(() => expect(revokeMutateAsync).toHaveBeenCalledWith('grant-active'));
    expect(await screen.findByText(/sharing revoked/i)).toBeInTheDocument();
  });

  it('surfaces grant conflicts without dark-pattern share-all copy', async () => {
    const user = userEvent.setup();
    createMutateAsync.mockRejectedValue(
      new ApiError('conflict', {
        category: 'CONFLICT',
        status: 409,
        code: 'ACTIVE_GRANT_EXISTS',
      }),
    );
    mockTeamsState = {
      isLoading: false,
      isError: false,
      data: [teamFixture],
      refetch: teamsRefetch,
    };
    mockGrantsState = { isLoading: false, isError: false, data: [], refetch: grantsRefetch };

    renderWithProviders(<SharingPage />);
    expect(screen.queryByRole('button', { name: /share all/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('checkbox', { name: /share all/i })).not.toBeInTheDocument();
    expect(screen.getByText(/there is no .share all. shortcut/i)).toBeInTheDocument();
    await user.selectOptions(screen.getByLabelText('Team'), teamFixture.teamId);
    await user.click(screen.getByRole('checkbox', { name: /availability/i }));
    await user.click(screen.getByRole('button', { name: 'Grant selected access' }));
    expect(await screen.findByText(/already have an active sharing grant/i)).toBeInTheDocument();
  });
});
