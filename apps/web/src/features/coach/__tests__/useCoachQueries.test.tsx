import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { type ReactNode } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { parseDateOnly } from '@/core/date/dateOnly';
import {
  useCoachAssignments,
  useCoachAthleteOverview,
  useCoachOrganizationTeams,
  useCoachOrganizations,
  useCreateCoachAssignment,
  useTeamReadiness,
  useTeamRoster,
} from '@/features/coach/hooks/useCoachQueries';
import { coachKeys } from '@/features/coach/models/queryKeys';
import { act, renderHook, waitFor } from '@/test/utils';

const fetchCoachOrganizations = vi.fn();
const fetchCoachOrganizationTeams = vi.fn();
const fetchTeamRoster = vi.fn();
const fetchCoachAthleteOverview = vi.fn();
const fetchCoachAssignments = vi.fn();
const createCoachAssignment = vi.fn();
const fetchTeamReadiness = vi.fn();

vi.mock('@/features/coach/api/coachApi', () => ({
  fetchCoachOrganizations: (...args: unknown[]) => fetchCoachOrganizations(...args),
  fetchCoachOrganizationTeams: (...args: unknown[]) => fetchCoachOrganizationTeams(...args),
  fetchTeamRoster: (...args: unknown[]) => fetchTeamRoster(...args),
  fetchCoachAthleteOverview: (...args: unknown[]) => fetchCoachAthleteOverview(...args),
  fetchCoachAssignments: (...args: unknown[]) => fetchCoachAssignments(...args),
  createCoachAssignment: (...args: unknown[]) => createCoachAssignment(...args),
  fetchTeamReadiness: (...args: unknown[]) => fetchTeamReadiness(...args),
}));

vi.mock('@/app/providers/AuthSessionProvider', () => ({
  useAuthSession: () => ({
    apiClient: { axios: {} },
    status: 'AUTHENTICATED',
    account: { accountId: 'acc-1', email: 'coach@example.com' },
  }),
}));

function wrapper(queryClient: QueryClient) {
  return function Wrapper({ children }: { children: ReactNode }) {
    return <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>;
  };
}

describe('useCoachQueries', () => {
  beforeEach(() => {
    fetchCoachOrganizations.mockReset();
    fetchCoachOrganizationTeams.mockReset();
    fetchTeamRoster.mockReset();
    fetchCoachAthleteOverview.mockReset();
    fetchCoachAssignments.mockReset();
    createCoachAssignment.mockReset();
    fetchTeamReadiness.mockReset();
  });

  it('loads organizations under account-scoped keys', async () => {
    fetchCoachOrganizations.mockResolvedValue([{ id: 'org-1', name: 'Devino' }]);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    const { result } = renderHook(() => useCoachOrganizations(), {
      wrapper: wrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.id).toBe('org-1');
    expect(queryClient.getQueryData(coachKeys.organizationList('acc-1'))).toBeDefined();
  });

  it('loads organization teams under account-scoped keys', async () => {
    fetchCoachOrganizationTeams.mockResolvedValue([{ id: 'team-1', name: 'Varsity' }]);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    const { result } = renderHook(() => useCoachOrganizationTeams('org-1'), {
      wrapper: wrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.[0]?.name).toBe('Varsity');
    expect(queryClient.getQueryData(coachKeys.teamList('acc-1', 'org-1'))).toBeDefined();
  });

  it('clears roster cache on 404', async () => {
    fetchTeamRoster.mockRejectedValue(
      new ApiError('missing', { category: 'NOT_FOUND', status: 404 }),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-1'), [{ athleteId: 'ath-1' }]);

    const { result } = renderHook(() => useTeamRoster('team-1'), {
      wrapper: wrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    await waitFor(() =>
      expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-1'))).toBeUndefined(),
    );
  });

  it('clears all coach queries on 401', async () => {
    fetchCoachAthleteOverview.mockRejectedValue(
      new ApiError('expired', { category: 'UNAUTHORIZED', status: 401 }),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const date = parseDateOnly('2026-09-07');
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-1'), [{ athleteId: 'ath-1' }]);

    const { result } = renderHook(() => useCoachAthleteOverview('team-1', 'ath-1', date), {
      wrapper: wrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    await waitFor(() =>
      expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-1'))).toBeUndefined(),
    );
  });

  it('does not keep previous team roster as placeholder data', async () => {
    fetchTeamRoster.mockResolvedValueOnce([
      {
        athleteId: 'ath-a',
        membershipId: 'mem-a',
        displayName: 'Team A',
        role: 'ATHLETE',
        status: 'ACTIVE',
      },
    ]);
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });

    const { result, rerender } = renderHook(
      ({ teamId }: { teamId: string }) => useTeamRoster(teamId),
      {
        wrapper: wrapper(queryClient),
        initialProps: { teamId: 'team-a' },
      },
    );

    await waitFor(() => expect(result.current.data?.[0]?.displayName).toBe('Team A'));

    fetchTeamRoster.mockImplementation(
      () =>
        new Promise((resolve) => {
          setTimeout(
            () =>
              resolve([
                {
                  athleteId: 'ath-b',
                  membershipId: 'mem-b',
                  displayName: 'Team B',
                  role: 'ATHLETE',
                  status: 'ACTIVE',
                },
              ]),
            30,
          );
        }),
    );

    act(() => {
      rerender({ teamId: 'team-b' });
    });

    expect(result.current.data).toBeUndefined();
    await waitFor(() => expect(result.current.data?.[0]?.displayName).toBe('Team B'));
  });

  it('loads assignments under account, team, and athlete keys and clears on 404', async () => {
    fetchCoachAssignments.mockRejectedValue(
      new ApiError('missing', { category: 'NOT_FOUND', status: 404 }),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    queryClient.setQueryData(coachKeys.assignments('acc-1', 'team-1', 'ath-1'), [{ id: 'stale' }]);

    const { result } = renderHook(() => useCoachAssignments('team-1', 'ath-1', true), {
      wrapper: wrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(queryClient.getQueryData(coachKeys.assignments('acc-1', 'team-1', 'ath-1'))).toBeUndefined();
  });

  it('creates an assignment and invalidates the assignment query', async () => {
    fetchCoachAssignments.mockResolvedValue([]);
    createCoachAssignment.mockResolvedValue({
      id: 'asg-1',
      title: 'Tempo intervals',
      status: 'ASSIGNED',
      provenance: 'COACH_ASSIGNMENT',
    });
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const invalidate = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useCreateCoachAssignment('team-1', 'ath-1'), {
      wrapper: wrapper(queryClient),
    });

    await act(async () => {
      await result.current.mutateAsync({
        title: 'Tempo intervals',
        description: '',
        scheduledDate: '2026-09-07',
        idempotencyKey: 'key-1',
      });
    });

    expect(createCoachAssignment).toHaveBeenCalled();
    expect(invalidate).toHaveBeenCalledWith({
      queryKey: coachKeys.assignments('acc-1', 'team-1', 'ath-1'),
    });
  });

  it('loads team readiness under account, team, and date keys', async () => {
    fetchTeamReadiness.mockResolvedValue({
      teamId: 'team-1',
      date: '2026-09-07',
      status: 'INSUFFICIENT_DATA',
      cohort: 'BELOW_MINIMUM',
      categoryDistribution: { status: 'INSUFFICIENT_DATA', cohort: 'BELOW_MINIMUM', cells: [] },
      limitingDimensionDistribution: { status: 'INSUFFICIENT_DATA', cohort: 'BELOW_MINIMUM', cells: [] },
      availability: { status: 'UNSUPPORTED' },
    });
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const date = parseDateOnly('2026-09-07');

    const { result } = renderHook(() => useTeamReadiness('team-1', date), {
      wrapper: wrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(fetchTeamReadiness).toHaveBeenCalled();
    expect(queryClient.getQueryData(coachKeys.teamReadiness('acc-1', 'team-1', date))).toBeDefined();
  });

  it('clears team readiness cache on 404', async () => {
    fetchTeamReadiness.mockRejectedValue(
      new ApiError('missing', { category: 'NOT_FOUND', status: 404 }),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const date = parseDateOnly('2026-09-07');
    queryClient.setQueryData(coachKeys.teamReadiness('acc-1', 'team-1', date), { stale: true });
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-1'), [{ athleteId: 'ath-1' }]);

    const { result } = renderHook(() => useTeamReadiness('team-1', date), {
      wrapper: wrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(queryClient.getQueryData(coachKeys.teamReadiness('acc-1', 'team-1', date))).toBeUndefined();
    expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-1'))).toBeUndefined();
  });

  it('clears all coach queries when team readiness returns 401', async () => {
    fetchTeamReadiness.mockRejectedValue(
      new ApiError('expired', { category: 'UNAUTHORIZED', status: 401 }),
    );
    const queryClient = new QueryClient({ defaultOptions: { queries: { retry: false } } });
    const date = parseDateOnly('2026-09-07');
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-1'), [{ athleteId: 'ath-1' }]);
    queryClient.setQueryData(coachKeys.organizationList('acc-1'), [{ id: 'org-1' }]);

    const { result } = renderHook(() => useTeamReadiness('team-1', date), {
      wrapper: wrapper(queryClient),
    });

    await waitFor(() => expect(result.current.isError).toBe(true));
    await waitFor(() => {
      expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-1'))).toBeUndefined();
      expect(queryClient.getQueryData(coachKeys.organizationList('acc-1'))).toBeUndefined();
    });
  });
});
