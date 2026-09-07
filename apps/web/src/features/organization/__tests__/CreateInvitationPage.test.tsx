import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { CreateInvitationPage } from '@/features/organization/pages/CreateInvitationPage';
import { renderWithProviders, screen, userEvent, waitFor } from '@/test/utils';

const refetchOrgs = vi.fn();
const createOrgMutateAsync = vi.fn();
const createTeamMutateAsync = vi.fn();

let mockOrganizationsState: {
  isLoading: boolean;
  isError: boolean;
  data: unknown;
  error?: unknown;
  refetch: typeof refetchOrgs;
};

let mockTeamsState: {
  isLoading: boolean;
  isError: boolean;
  data: unknown;
  error?: unknown;
};

vi.mock('@/features/organization/hooks/useOrganizations', () => ({
  useMyOrganizations: () => mockOrganizationsState,
  useOrganizationTeams: () => mockTeamsState,
}));

vi.mock('@/features/organization/hooks/useInvitationMutations', () => ({
  useCreateOrganizationInvitationMutation: () => ({
    mutateAsync: createOrgMutateAsync,
    isPending: false,
  }),
  useCreateTeamInvitationMutation: () => ({
    mutateAsync: createTeamMutateAsync,
    isPending: false,
  }),
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: { axios: {} },
    status: 'AUTHENTICATED',
  }),
}));

const organizations = [
  {
    id: 'org-1',
    name: 'Devino Labs',
    status: 'ACTIVE',
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
    version: 0,
  },
];

const teams = [
  {
    id: 'team-1',
    organizationId: 'org-1',
    name: 'Varsity',
    status: 'ACTIVE',
    createdAt: '2026-09-01T00:00:00Z',
    updatedAt: '2026-09-01T00:00:00Z',
    version: 0,
  },
];

function invitationResponse(overrides: Record<string, unknown> = {}) {
  return {
    id: 'inv-1',
    organizationId: 'org-1',
    teamId: null,
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
    rawToken: 'one-time-raw-token',
    ...overrides,
  };
}

function readyFormState() {
  mockOrganizationsState = {
    isLoading: false,
    isError: false,
    data: organizations,
    refetch: refetchOrgs,
  };
  mockTeamsState = {
    isLoading: false,
    isError: false,
    data: teams,
  };
}

describe('CreateInvitationPage', () => {
  beforeEach(() => {
    createOrgMutateAsync.mockReset();
    createTeamMutateAsync.mockReset();
    refetchOrgs.mockReset();
  });

  it('shows loading, load error, and empty organization states', () => {
    mockOrganizationsState = {
      isLoading: true,
      isError: false,
      data: undefined,
      refetch: refetchOrgs,
    };
    mockTeamsState = { isLoading: false, isError: false, data: [] };
    const { unmount } = renderWithProviders(<CreateInvitationPage />);
    expect(screen.getByText('Loading organizations…')).toBeInTheDocument();
    unmount();

    mockOrganizationsState = {
      isLoading: false,
      isError: true,
      data: undefined,
      error: new ApiError('boom', { category: 'SERVER', status: 500, code: 'SERVER_ERROR' }),
      refetch: refetchOrgs,
    };
    const errored = renderWithProviders(<CreateInvitationPage />);
    expect(screen.getByRole('alert')).toHaveTextContent('boom');
    expect(screen.getByRole('button', { name: 'Try again' })).toBeInTheDocument();
    errored.unmount();

    mockOrganizationsState = {
      isLoading: false,
      isError: false,
      data: [],
      refetch: refetchOrgs,
    };
    renderWithProviders(<CreateInvitationPage />);
    expect(screen.getByText('No organizations')).toBeInTheDocument();
  });

  it('creates a team invitation and shows the rawToken once', async () => {
    const user = userEvent.setup();
    readyFormState();
    createTeamMutateAsync.mockResolvedValue(
      invitationResponse({
        teamId: 'team-1',
        role: 'ATHLETE',
        rawToken: 'team-raw-token-once',
      }),
    );

    renderWithProviders(<CreateInvitationPage />);

    await user.selectOptions(screen.getByLabelText('Organization'), 'org-1');
    await user.selectOptions(screen.getByLabelText('Team (optional)'), 'team-1');
    await waitFor(() => expect(screen.getByLabelText('Role')).toHaveValue('ATHLETE'));
    await user.type(screen.getByLabelText('Email'), 'athlete@example.com');
    await user.click(screen.getByRole('button', { name: 'Create invitation' }));

    await waitFor(() =>
      expect(createTeamMutateAsync).toHaveBeenCalledWith({
        teamId: 'team-1',
        request: { email: 'athlete@example.com', role: 'ATHLETE' },
      }),
    );
    expect(createOrgMutateAsync).not.toHaveBeenCalled();

    expect(await screen.findByText('team-raw-token-once')).toBeInTheDocument();
    expect(screen.getByRole('status')).toHaveTextContent(/Invitation created/i);
    expect(screen.getByText(/\/app\/invitations\/token\/team-raw-token-once/)).toBeInTheDocument();
  });

  it('creates an organization-level invitation', async () => {
    const user = userEvent.setup();
    readyFormState();
    createOrgMutateAsync.mockResolvedValue(
      invitationResponse({
        role: 'ORG_ADMIN',
        rawToken: 'org-raw-token-once',
      }),
    );

    renderWithProviders(<CreateInvitationPage />);

    await user.selectOptions(screen.getByLabelText('Organization'), 'org-1');
    await user.type(screen.getByLabelText('Email'), 'admin@example.com');
    await waitFor(() => expect(screen.getByLabelText('Role')).toHaveValue('ORG_ADMIN'));
    await user.click(screen.getByRole('button', { name: 'Create invitation' }));

    await waitFor(() =>
      expect(createOrgMutateAsync).toHaveBeenCalledWith({
        organizationId: 'org-1',
        request: { email: 'admin@example.com', role: 'ORG_ADMIN' },
      }),
    );
    expect(createTeamMutateAsync).not.toHaveBeenCalled();
    expect(await screen.findByText('org-raw-token-once')).toBeInTheDocument();
  });

  it('requires an organization before submit', async () => {
    const user = userEvent.setup();
    readyFormState();
    renderWithProviders(<CreateInvitationPage />);

    await user.type(screen.getByLabelText('Email'), 'athlete@example.com');
    await user.click(screen.getByRole('button', { name: 'Create invitation' }));

    expect(createOrgMutateAsync).not.toHaveBeenCalled();
    expect(createTeamMutateAsync).not.toHaveBeenCalled();
  });

  it('surfaces create errors and missing rawToken responses', async () => {
    const user = userEvent.setup();
    readyFormState();
    createOrgMutateAsync.mockRejectedValueOnce(
      new ApiError('forbidden', {
        category: 'FORBIDDEN',
        status: 403,
        code: 'FORBIDDEN',
      }),
    );

    renderWithProviders(<CreateInvitationPage />);
    await user.selectOptions(screen.getByLabelText('Organization'), 'org-1');
    await user.type(screen.getByLabelText('Email'), 'admin@example.com');
    await waitFor(() => expect(screen.getByLabelText('Role')).toHaveValue('ORG_ADMIN'));
    await user.click(screen.getByRole('button', { name: 'Create invitation' }));

    expect(await screen.findByRole('alert')).toHaveTextContent('forbidden');

    createOrgMutateAsync.mockResolvedValueOnce(invitationResponse({ rawToken: null }));
    await user.click(screen.getByRole('button', { name: 'Create invitation' }));
    expect(
      await screen.findByText('Invitation created, but no one-time token was returned.'),
    ).toBeInTheDocument();
  });

  it('surfaces team load errors without blocking the form', async () => {
    const user = userEvent.setup();
    readyFormState();
    mockTeamsState = {
      isLoading: false,
      isError: true,
      data: undefined,
      error: new ApiError('teams failed', { category: 'SERVER', status: 500, code: 'SERVER_ERROR' }),
    };

    renderWithProviders(<CreateInvitationPage />);
    await user.selectOptions(screen.getByLabelText('Organization'), 'org-1');
    expect(await screen.findByRole('alert')).toHaveTextContent('teams failed');
  });
});
