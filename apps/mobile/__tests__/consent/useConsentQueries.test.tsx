import { renderHook, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PropsWithChildren } from 'react';

import { useMyAthleteTeamMemberships } from '@/src/features/consent/hooks/useMyAthleteTeamMemberships';
import { useMyConsents } from '@/src/features/consent/hooks/useMyConsents';
import { consentKeys } from '@/src/features/consent/models/queryKeys';

const mockApiClient = { axios: {} };
const mockListConsents = jest.fn();
const mockListMemberships = jest.fn();

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({ apiClient: mockApiClient, status: 'AUTHENTICATED' }),
}));

jest.mock('@/src/features/consent/api/consentsApi', () => ({
  listMyConsents: (...args: unknown[]) => mockListConsents(...args),
  listMyAthleteTeamMemberships: (...args: unknown[]) => mockListMemberships(...args),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });

  function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  }

  return { Wrapper, queryClient };
}

describe('consent query hooks', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockListConsents.mockResolvedValue([]);
    mockListMemberships.mockResolvedValue([]);
  });

  it('loads my consents with consentKeys.mine', async () => {
    const { Wrapper } = createWrapper();
    const { result } = await renderHook(() => useMyConsents(), { wrapper: Wrapper });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(mockListConsents).toHaveBeenCalledWith(mockApiClient);
    expect(result.current.data).toEqual([]);
  });

  it('loads athlete team memberships with consentKeys.teamMemberships', async () => {
    mockListMemberships.mockResolvedValue([
      {
        membershipId: 'tm-1',
        teamId: 'team-1',
        teamName: 'Varsity',
        organizationId: 'org-1',
        organizationName: 'Org',
        athleteId: 'ath-1',
      },
    ]);
    const { Wrapper } = createWrapper();
    const { result } = await renderHook(() => useMyAthleteTeamMemberships(), {
      wrapper: Wrapper,
    });

    await waitFor(() => {
      expect(result.current.isSuccess).toBe(true);
    });
    expect(mockListMemberships).toHaveBeenCalledWith(mockApiClient);
    expect(result.current.data?.[0]?.teamName).toBe('Varsity');
    expect(consentKeys.teamMemberships()).toEqual(['consent', 'team-memberships', 'mine']);
    expect(consentKeys.all).toEqual(['consent']);
  });
});
