import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { renderHook, waitFor } from '@testing-library/react';
import type { PropsWithChildren } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import {
  useCreateOrganizationInvitationMutation,
  useCreateTeamInvitationMutation,
} from '@/features/organization/hooks/useInvitationMutations';
import { organizationKeys } from '@/features/organization/models/queryKeys';

const createOrganizationInvitation = vi.fn();
const createTeamInvitation = vi.fn();

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({ apiClient: { axios: {} }, status: 'AUTHENTICATED' }),
}));

vi.mock('@/features/organization/api/invitationsApi', () => ({
  acceptInvitationById: vi.fn(),
  acceptInvitationByToken: vi.fn(),
  createOrganizationInvitation: (...args: unknown[]) => createOrganizationInvitation(...args),
  createTeamInvitation: (...args: unknown[]) => createTeamInvitation(...args),
  declineInvitationById: vi.fn(),
  declineInvitationByToken: vi.fn(),
}));

function invitationResponse(overrides: Record<string, unknown> = {}) {
  return {
    id: 'inv-1',
    organizationId: 'org-1',
    teamId: null,
    invitedEmail: 'athlete@example.com',
    invitedAccountId: null,
    role: 'ORG_ADMIN',
    status: 'PENDING',
    expiresAt: '2026-09-13T12:00:00Z',
    acceptedMembershipId: null,
    createdByAccountId: 'acc-1',
    createdAt: '2026-09-06T12:00:00Z',
    updatedAt: '2026-09-06T12:00:00Z',
    version: 0,
    rawToken: 'one-time-token',
    ...overrides,
  };
}

describe('useInvitationMutations create hooks', () => {
  beforeEach(() => {
    createOrganizationInvitation.mockReset();
    createTeamInvitation.mockReset();
  });

  it('creates an organization invitation and invalidates invitation/org queries', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
    createOrganizationInvitation.mockResolvedValue(invitationResponse());

    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useCreateOrganizationInvitationMutation(), { wrapper });

    await result.current.mutateAsync({
      organizationId: 'org-1',
      request: { email: 'admin@example.com', role: 'ORG_ADMIN' },
    });

    expect(createOrganizationInvitation).toHaveBeenCalledWith(
      expect.anything(),
      'org-1',
      { email: 'admin@example.com', role: 'ORG_ADMIN' },
    );
    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: organizationKeys.invitations() });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: organizationKeys.organizations() });
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: organizationKeys.teams() });
    });
  });

  it('creates a team invitation and invalidates invitation/org queries', async () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
    });
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');
    createTeamInvitation.mockResolvedValue(
      invitationResponse({ teamId: 'team-1', role: 'ATHLETE' }),
    );

    const wrapper = ({ children }: PropsWithChildren) => (
      <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
    );
    const { result } = renderHook(() => useCreateTeamInvitationMutation(), { wrapper });

    await result.current.mutateAsync({
      teamId: 'team-1',
      request: { email: 'athlete@example.com', role: 'ATHLETE' },
    });

    expect(createTeamInvitation).toHaveBeenCalledWith(expect.anything(), 'team-1', {
      email: 'athlete@example.com',
      role: 'ATHLETE',
    });
    await waitFor(() => {
      expect(invalidateSpy).toHaveBeenCalledWith({ queryKey: organizationKeys.invitations() });
    });
  });
});
