import { act, renderHook, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PropsWithChildren } from 'react';

import { useConsentMutations } from '@/src/features/consent/hooks/useConsentMutations';
import { ConsentGrant } from '@/src/features/consent/models/consentSchemas';
import { consentKeys } from '@/src/features/consent/models/queryKeys';

const mockApiClient = { axios: {} };

const mockCreate = jest.fn();
const mockRevoke = jest.fn();

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({ apiClient: mockApiClient }),
}));

jest.mock('@/src/features/consent/api/consentsApi', () => ({
  createConsentGrant: (...args: unknown[]) => mockCreate(...args),
  revokeConsentGrant: (...args: unknown[]) => mockRevoke(...args),
}));

const activeGrant: ConsentGrant = {
  id: 'cg-1',
  athleteId: 'ath-1',
  teamId: 'team-1',
  organizationId: 'org-1',
  teamMembershipId: 'tm-1',
  scopes: ['AVAILABILITY'],
  status: 'ACTIVE',
  createdAt: '2026-09-06T12:00:00Z',
  revokedAt: null,
  updatedAt: '2026-09-06T12:00:00Z',
  version: 0,
};

function createWrapper(seed?: ConsentGrant[]) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  if (seed) {
    queryClient.setQueryData(consentKeys.mine(), seed);
  }
  const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

  function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  }

  return { Wrapper, queryClient, invalidateSpy };
}

describe('useConsentMutations', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockCreate.mockResolvedValue(activeGrant);
    mockRevoke.mockResolvedValue(undefined);
  });

  it('creates a grant and invalidates consent queries', async () => {
    const { Wrapper, invalidateSpy } = createWrapper();
    const { result } = await renderHook(() => useConsentMutations(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      await result.current.createMutation.mutateAsync({
        teamId: '00000000-0000-4000-8000-000000000001',
        scopes: ['AVAILABILITY'],
      });
    });

    expect(mockCreate).toHaveBeenCalledWith(mockApiClient, {
      teamId: '00000000-0000-4000-8000-000000000001',
      scopes: ['AVAILABILITY'],
    });
    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: consentKeys.mine(),
      });
    });
  });

  it('optimistically hides a revoked grant and restores on failure', async () => {
    const { Wrapper, queryClient } = createWrapper([activeGrant]);
    mockRevoke.mockRejectedValueOnce(new Error('network'));

    const { result } = await renderHook(() => useConsentMutations(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      try {
        await result.current.revokeMutation.mutateAsync('cg-1');
      } catch {
        // expected
      }
    });

    await waitFor(() => {
      expect(queryClient.getQueryData(consentKeys.mine())).toEqual([activeGrant]);
    });
  });

  it('optimistically removes grant on successful revoke path', async () => {
    const { Wrapper, queryClient, invalidateSpy } = createWrapper([activeGrant]);
    const { result } = await renderHook(() => useConsentMutations(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      const pending = result.current.revokeMutation.mutateAsync('cg-1');
      await waitFor(() => {
        expect(queryClient.getQueryData(consentKeys.mine())).toEqual([]);
      });
      await pending;
    });

    expect(mockRevoke).toHaveBeenCalledWith(mockApiClient, 'cg-1');
    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: consentKeys.mine(),
      });
    });
  });
});
