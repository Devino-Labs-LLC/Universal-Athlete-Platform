import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { useMyOrganizations, useOrganizationTeams } from '@/features/organization/hooks/useOrganizations';
import { organizationKeys } from '@/features/organization/models/queryKeys';

const fetchMyOrganizations = vi.fn();
const fetchOrganizationTeams = vi.fn();

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({ apiClient: { axios: {} }, status: 'AUTHENTICATED' }),
}));

vi.mock('@/features/organization/api/organizationsApi', () => ({
  fetchMyOrganizations: (...args: unknown[]) => fetchMyOrganizations(...args),
  fetchOrganizationTeams: (...args: unknown[]) => fetchOrganizationTeams(...args),
}));

function wrapperFor(queryClient: QueryClient) {
  return ({ children }: PropsWithChildren) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );
}

describe('useOrganizations hooks', () => {
  beforeEach(() => {
    fetchMyOrganizations.mockReset();
    fetchOrganizationTeams.mockReset();
  });

  it('loads my organizations when authenticated', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    fetchMyOrganizations.mockResolvedValue([
      {
        id: 'org-1',
        name: 'Devino Labs',
        status: 'ACTIVE',
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-01T00:00:00Z',
        version: 0,
      },
    ]);

    const { result } = renderHook(() => useMyOrganizations(), {
      wrapper: wrapperFor(queryClient),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchMyOrganizations).toHaveBeenCalledTimes(1);
    expect(queryClient.getQueryData(organizationKeys.organizationList())).toEqual([
      expect.objectContaining({ id: 'org-1', name: 'Devino Labs' }),
    ]);
  });

  it('loads teams only when an organization id is provided', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });
    fetchOrganizationTeams.mockResolvedValue([
      {
        id: 'team-1',
        organizationId: 'org-1',
        name: 'Varsity',
        status: 'ACTIVE',
        createdAt: '2026-09-01T00:00:00Z',
        updatedAt: '2026-09-01T00:00:00Z',
        version: 0,
      },
    ]);

    const { result: disabled } = renderHook(() => useOrganizationTeams(null), {
      wrapper: wrapperFor(queryClient),
    });
    expect(disabled.current.fetchStatus).toBe('idle');
    expect(fetchOrganizationTeams).not.toHaveBeenCalled();

    const { result } = renderHook(() => useOrganizationTeams('org-1'), {
      wrapper: wrapperFor(queryClient),
    });
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchOrganizationTeams).toHaveBeenCalledWith(expect.anything(), 'org-1');
    expect(queryClient.getQueryData(organizationKeys.teamList('org-1'))).toEqual([
      expect.objectContaining({ id: 'team-1', name: 'Varsity' }),
    ]);
  });
});
