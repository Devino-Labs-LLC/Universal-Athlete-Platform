import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  useCreateConsentGrantMutation,
  useRevokeConsentGrantMutation,
} from '@/features/consent/hooks/useConsentMutations';
import { consentKeys } from '@/features/consent/models/queryKeys';

const createConsentGrant = vi.fn();
const revokeConsentGrant = vi.fn();

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({ apiClient: { axios: {} }, status: 'AUTHENTICATED' }),
}));

vi.mock('@/features/consent/api/consentsApi', () => ({
  createConsentGrant: (...args: unknown[]) => createConsentGrant(...args),
  revokeConsentGrant: (...args: unknown[]) => revokeConsentGrant(...args),
  fetchMyConsentGrants: vi.fn(),
}));

describe('useConsentMutations', () => {
  beforeEach(() => {
    createConsentGrant.mockReset();
    revokeConsentGrant.mockReset();
  });

  it('creates a grant and invalidates consent queries', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
    createConsentGrant.mockResolvedValue({
      id: 'grant-1',
      athleteId: 'ath-1',
      teamId: '33333333-3333-3333-3333-333333333333',
      organizationId: 'org-1',
      teamMembershipId: 'mem-1',
      scopes: ['AVAILABILITY'],
      status: 'ACTIVE',
      createdAt: '2026-09-07T12:00:00Z',
      revokedAt: null,
      updatedAt: '2026-09-07T12:00:00Z',
      version: 0,
    });

    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useCreateConsentGrantMutation(), { wrapper });

    await result.current.mutateAsync({
      teamId: '33333333-3333-3333-3333-333333333333',
      scopes: ['AVAILABILITY'],
    });

    expect(createConsentGrant).toHaveBeenCalledWith(expect.anything(), {
      teamId: '33333333-3333-3333-3333-333333333333',
      scopes: ['AVAILABILITY'],
    });
    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: consentKeys.grants() });
    });
  });

  it('revokes a grant and invalidates consent queries', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
    revokeConsentGrant.mockResolvedValue(undefined);

    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useRevokeConsentGrantMutation(), { wrapper });

    await result.current.mutateAsync('grant-1');
    expect(revokeConsentGrant).toHaveBeenCalledWith(expect.anything(), 'grant-1');
    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: consentKeys.grants() });
    });
  });
});
