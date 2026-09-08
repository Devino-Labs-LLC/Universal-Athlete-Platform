import { beforeEach, describe, expect, it, vi } from 'vitest';

import type { ApiClient } from '@/core/api/apiClient';
import { parseDateOnly } from '@/core/date/dateOnly';
import {
  createCoachAssignment,
  fetchCoachAssignments,
  fetchCoachAthleteOverview,
  fetchCoachOrganizationTeams,
  fetchCoachOrganizations,
  fetchTeamReadiness,
  fetchTeamRoster,
} from '@/features/coach/api/coachApi';

const get = vi.fn();
const post = vi.fn();

const client = {
  axios: { get, post },
} as unknown as ApiClient;

vi.mock('@/features/organization/api/organizationsApi', () => ({
  fetchMyOrganizations: vi.fn(async () => [{ id: 'org-1', name: 'Devino' }]),
  fetchOrganizationTeams: vi.fn(async () => [{ id: 'team-1', name: 'Varsity' }]),
}));

describe('coachApi', () => {
  beforeEach(() => {
    get.mockReset();
  });

  it('delegates organization and team list fetches', async () => {
    await expect(fetchCoachOrganizations(client)).resolves.toEqual([
      { id: 'org-1', name: 'Devino' },
    ]);
    await expect(fetchCoachOrganizationTeams(client, 'org-1')).resolves.toEqual([
      { id: 'team-1', name: 'Varsity' },
    ]);
  });

  it('fetches and parses team roster', async () => {
    get.mockResolvedValue({
      data: [
        {
          athleteId: 'ath-1',
          membershipId: 'mem-1',
          displayName: 'Alex Runner',
          role: 'ATHLETE',
          status: 'ACTIVE',
        },
      ],
    });

    const roster = await fetchTeamRoster(client, 'team-1');
    expect(get).toHaveBeenCalledWith('/api/v1/teams/team-1/roster');
    expect(roster[0]?.displayName).toBe('Alex Runner');
  });

  it('fetches athlete overview with optional date param', async () => {
    get.mockResolvedValue({
      data: {
        teamId: 'team-1',
        organizationId: 'org-1',
        athleteId: 'ath-1',
        membershipId: 'mem-1',
        displayName: 'Alex Runner',
        role: 'ATHLETE',
        viewDate: '2026-09-07',
        effectiveScopes: [],
        availability: { status: 'NOT_SHARED', data: null },
        readinessCategory: { status: 'NOT_SHARED', data: null },
        readinessScore: { status: 'NOT_SHARED', data: null },
        limitingDimensions: { status: 'NOT_SHARED', data: null },
        recoveryCheckIn: { status: 'NOT_SHARED', data: null },
        trainingAdherence: { status: 'NOT_SHARED', data: null },
        performanceHistory: { status: 'NOT_SHARED', data: null },
      },
    });

    const date = parseDateOnly('2026-09-07');
    const overview = await fetchCoachAthleteOverview(client, 'team-1', 'ath-1', date);
    expect(get).toHaveBeenCalledWith('/api/v1/teams/team-1/athletes/ath-1/overview', {
      params: { date },
    });
    expect(overview.displayName).toBe('Alex Runner');
  });

  it('omits date param when not provided', async () => {
    get.mockResolvedValue({
      data: {
        teamId: 'team-1',
        organizationId: 'org-1',
        athleteId: 'ath-1',
        membershipId: 'mem-1',
        displayName: 'Alex Runner',
        role: 'ATHLETE',
        viewDate: '2026-09-07',
        effectiveScopes: [],
        availability: { status: 'NOT_SHARED', data: null },
        readinessCategory: { status: 'NOT_SHARED', data: null },
        readinessScore: { status: 'NOT_SHARED', data: null },
        limitingDimensions: { status: 'NOT_SHARED', data: null },
        recoveryCheckIn: { status: 'NOT_SHARED', data: null },
        trainingAdherence: { status: 'NOT_SHARED', data: null },
        performanceHistory: { status: 'NOT_SHARED', data: null },
      },
    });
    await fetchCoachAthleteOverview(client, 'team-1', 'ath-1');
    expect(get).toHaveBeenCalledWith('/api/v1/teams/team-1/athletes/ath-1/overview', {
      params: undefined,
    });
  });

  it('fetches and creates coach assignments without readiness fields', async () => {
    const assignment = {
      id: 'asg-1',
      teamId: 'team-1',
      athleteId: 'ath-1',
      title: 'Tempo intervals',
      description: null,
      scheduledDate: '2026-09-07',
      status: 'ASSIGNED',
      athleteResponseNote: null,
      respondedAt: null,
      provenance: 'COACH_ASSIGNMENT',
      assignedByRole: 'COACH',
      version: 0,
    };
    get.mockResolvedValue({ data: [assignment] });
    post.mockResolvedValue({ data: assignment });

    await expect(fetchCoachAssignments(client, 'team-1', 'ath-1')).resolves.toEqual([assignment]);
    await expect(
      createCoachAssignment(client, 'team-1', 'ath-1', {
        title: 'Tempo intervals',
        description: '',
        scheduledDate: '2026-09-07',
        idempotencyKey: 'key-1',
      }),
    ).resolves.toEqual(assignment);
    expect(post).toHaveBeenCalledWith(
      '/api/v1/teams/team-1/athletes/ath-1/training/assignments',
      expect.objectContaining({ title: 'Tempo intervals', idempotencyKey: 'key-1' }),
    );
  });

  it('fetches team readiness for an explicit date without identity fields', async () => {
    get.mockResolvedValue({
      data: {
        teamId: 'team-1',
        date: '2026-09-07',
        status: 'PUBLISHED',
        cohort: 'AT_LEAST_MINIMUM',
        includedCount: null,
        categoryDistribution: {
          status: 'PUBLISHED',
          cohort: 'AT_LEAST_MINIMUM',
          cells: [{ category: 'HIGH', publication: 'SUPPRESSED' }],
        },
        limitingDimensionDistribution: {
          status: 'INSUFFICIENT_DATA',
          cohort: 'BELOW_MINIMUM',
          cells: [],
        },
        availability: { status: 'UNSUPPORTED' },
      },
    });

    const date = parseDateOnly('2026-09-07');
    const view = await fetchTeamReadiness(client, 'team-1', date);
    expect(get).toHaveBeenCalledWith('/api/v1/teams/team-1/readiness', { params: { date } });
    expect(view.status).toBe('PUBLISHED');
    expect(view).not.toHaveProperty('athleteId');
    expect(view.categoryDistribution.cells[0]?.publication).toBe('SUPPRESSED');
  });
});
