import { beforeEach, describe, expect, it, vi } from 'vitest';

import { CoachHomePage } from '@/features/coach/pages/CoachHomePage';
import { renderWithProviders, screen, userEvent } from '@/test/utils';

const navigate = vi.fn();
const refetchOrgs = vi.fn();

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

vi.mock('@/features/coach/hooks/useCoachQueries', () => ({
  useCoachOrganizations: () => mockOrganizationsState,
  useCoachOrganizationTeams: () => mockTeamsState,
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    account: { accountId: 'acc-1', email: 'coach@example.com', status: 'ACTIVE' },
    status: 'AUTHENTICATED',
    apiClient: { axios: {} },
  }),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => navigate,
  };
});

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

describe('CoachHomePage', () => {
  beforeEach(() => {
    navigate.mockReset();
    refetchOrgs.mockReset();
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
  });

  it('shows empty organizations', () => {
    mockOrganizationsState = {
      isLoading: false,
      isError: false,
      data: [],
      refetch: refetchOrgs,
    };
    renderWithProviders(<CoachHomePage />);
    expect(screen.getByText('No organizations')).toBeInTheDocument();
  });

  it('navigates to roster after selecting org and team', async () => {
    const user = userEvent.setup();
    renderWithProviders(<CoachHomePage />);

    await user.selectOptions(screen.getByLabelText('Organization'), 'org-1');
    await user.selectOptions(screen.getByLabelText('Team'), 'team-1');
    await user.click(screen.getByRole('button', { name: 'Open roster' }));

    expect(navigate).toHaveBeenCalledWith('/coach/teams/team-1/roster');
  });
});
