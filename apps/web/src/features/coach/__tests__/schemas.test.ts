import { describe, expect, it } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  coachAthleteOverviewSchema,
  teamRosterSchema,
} from '@/features/coach/models/schemas';

describe('teamRosterSchema', () => {
  it('parses roster-safe fields only', () => {
    const roster = teamRosterSchema.parse([
      {
        athleteId: 'ath-1',
        membershipId: 'mem-1',
        displayName: 'Alex Runner',
        role: 'ATHLETE',
        status: 'ACTIVE',
      },
    ]);
    expect(roster).toHaveLength(1);
    expect(roster[0]).toEqual({
      athleteId: 'ath-1',
      membershipId: 'mem-1',
      displayName: 'Alex Runner',
      role: 'ATHLETE',
      status: 'ACTIVE',
    });
  });

  it('rejects non-athlete roles', () => {
    expect(() =>
      teamRosterSchema.parse([
        {
          athleteId: 'ath-1',
          membershipId: 'mem-1',
          displayName: 'Coach',
          role: 'COACH',
          status: 'ACTIVE',
        },
      ]),
    ).toThrow();
  });
});

describe('coachAthleteOverviewSchema', () => {
  const base = {
    teamId: 'team-1',
    organizationId: 'org-1',
    athleteId: 'ath-1',
    membershipId: 'mem-1',
    displayName: 'Alex Runner',
    role: 'ATHLETE',
    viewDate: '2026-09-07',
    effectiveScopes: ['READINESS_CATEGORY'],
    availability: { status: 'NO_DATA', data: null },
    readinessCategory: {
      status: 'AVAILABLE',
      data: { readinessBand: 'READY', dataSufficiency: 'SUFFICIENT' },
    },
    readinessScore: { status: 'NOT_SHARED', data: null },
    limitingDimensions: { status: 'NOT_SHARED', data: null },
    recoveryCheckIn: { status: 'NOT_SHARED', data: null },
    trainingAdherence: { status: 'NOT_SHARED', data: null },
    performanceHistory: { status: 'NOT_SHARED', data: null },
  };

  it('parses overview and keeps readinessScore free of readinessBand', () => {
    const overview = coachAthleteOverviewSchema.parse({
      ...base,
      readinessScore: {
        status: 'AVAILABLE',
        data: {
          readinessScore: 72.5,
          dataSufficiency: 'SUFFICIENT',
          summaryReasonCode: 'WITHIN_BASELINE',
        },
      },
    });

    expect(overview.viewDate).toEqual(parseDateOnly('2026-09-07'));
    expect(overview.readinessScore.data).toEqual({
      readinessScore: 72.5,
      dataSufficiency: 'SUFFICIENT',
      summaryReasonCode: 'WITHIN_BASELINE',
    });
    expect(overview.readinessScore.data).not.toHaveProperty('readinessBand');
  });

  it('accepts independent section statuses', () => {
    const overview = coachAthleteOverviewSchema.parse(base);
    expect(overview.availability.status).toBe('NO_DATA');
    expect(overview.readinessCategory.status).toBe('AVAILABLE');
    expect(overview.readinessScore.status).toBe('NOT_SHARED');
  });
});
