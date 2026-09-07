import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { describe, expect, it, vi } from 'vitest';

import { useMyConsentGrants } from '@/features/consent/hooks/useMyConsentGrants';
import { consentKeys } from '@/features/consent/models/queryKeys';

const fetchMyConsentGrants = vi.fn();

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({ apiClient: { axios: {} }, status: 'AUTHENTICATED' }),
}));

vi.mock('@/features/consent/api/consentsApi', () => ({
  fetchMyConsentGrants: (...args: unknown[]) => fetchMyConsentGrants(...args),
  createConsentGrant: vi.fn(),
  revokeConsentGrant: vi.fn(),
}));

describe('useMyConsentGrants', () => {
  it('loads my consent grants into the query cache', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    fetchMyConsentGrants.mockResolvedValue([
      {
        id: 'grant-1',
        athleteId: 'ath-1',
        teamId: 'team-1',
        organizationId: 'org-1',
        teamMembershipId: 'mem-1',
        scopes: ['AVAILABILITY'],
        status: 'ACTIVE',
        createdAt: '2026-09-07T12:00:00Z',
        revokedAt: null,
        updatedAt: '2026-09-07T12:00:00Z',
        version: 0,
      },
    ]);

    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useMyConsentGrants(), { wrapper });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMyConsentGrants).toHaveBeenCalled();
    expect(queryClient.getQueryData(consentKeys.myGrants())).toEqual([
      expect.objectContaining({ id: 'grant-1', status: 'ACTIVE' }),
    ]);
  });
});
