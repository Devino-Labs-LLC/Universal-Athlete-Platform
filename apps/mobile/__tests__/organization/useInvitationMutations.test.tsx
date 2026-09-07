import { act, renderHook, waitFor } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { PropsWithChildren } from 'react';

import { useInvitationMutations } from '@/src/features/organization/hooks/useInvitationMutations';
import { invitationKeys } from '@/src/features/organization/models/queryKeys';

const mockApiClient = { axios: {} };

const mockAcceptById = jest.fn();
const mockDeclineById = jest.fn();
const mockAcceptByToken = jest.fn();
const mockDeclineByToken = jest.fn();

jest.mock('@/src/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({ apiClient: mockApiClient }),
}));

jest.mock('@/src/features/organization/api/invitationsApi', () => ({
  acceptInvitationById: (...args: unknown[]) => mockAcceptById(...args),
  declineInvitationById: (...args: unknown[]) => mockDeclineById(...args),
  acceptInvitationByToken: (...args: unknown[]) => mockAcceptByToken(...args),
  declineInvitationByToken: (...args: unknown[]) => mockDeclineByToken(...args),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  const invalidateSpy = jest.spyOn(queryClient, 'invalidateQueries');

  function Wrapper({ children }: PropsWithChildren) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  }

  return { Wrapper, queryClient, invalidateSpy };
}

describe('useInvitationMutations', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockAcceptById.mockResolvedValue({
      organizationMembership: null,
      teamMembership: null,
    });
    mockDeclineById.mockResolvedValue(undefined);
    mockAcceptByToken.mockResolvedValue({
      organizationMembership: null,
      teamMembership: null,
    });
    mockDeclineByToken.mockResolvedValue(undefined);
  });

  it('accepts and declines by id and invalidates my invitations', async () => {
    const { Wrapper, invalidateSpy } = createWrapper();
    const { result } = await renderHook(() => useInvitationMutations(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      await result.current.acceptByIdMutation.mutateAsync('inv-1');
    });
    expect(mockAcceptById).toHaveBeenCalledWith(mockApiClient, 'inv-1');

    await act(async () => {
      await result.current.declineByIdMutation.mutateAsync('inv-2');
    });
    expect(mockDeclineById).toHaveBeenCalledWith(mockApiClient, 'inv-2');

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: invitationKeys.mine(),
      });
    });
    expect(invalidateSpy.mock.calls.length).toBeGreaterThanOrEqual(2);
  });

  it('accepts and declines by token and invalidates my invitations', async () => {
    const { Wrapper, invalidateSpy } = createWrapper();
    const { result } = await renderHook(() => useInvitationMutations(), {
      wrapper: Wrapper,
    });

    await act(async () => {
      await result.current.acceptByTokenMutation.mutateAsync('raw+token');
    });
    expect(mockAcceptByToken).toHaveBeenCalledWith(mockApiClient, 'raw+token');

    await act(async () => {
      await result.current.declineByTokenMutation.mutateAsync('raw+token');
    });
    expect(mockDeclineByToken).toHaveBeenCalledWith(mockApiClient, 'raw+token');

    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({
        queryKey: invitationKeys.mine(),
      });
    });
    expect(invalidateSpy.mock.calls.length).toBeGreaterThanOrEqual(2);
  });
});
