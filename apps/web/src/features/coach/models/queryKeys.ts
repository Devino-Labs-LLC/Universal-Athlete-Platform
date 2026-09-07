import type { DateOnly } from '@/core/date/dateOnly';

/**
 * Coach cache keys always include accountId so Athlete A / Account A data
 * cannot collide with Account B on the same device. teamId and athleteId
 * further isolate roster and overview projections.
 */
export const coachKeys = {
  root: ['coach'] as const,

  all: (accountId: string) => [...coachKeys.root, accountId] as const,

  organizations: (accountId: string) =>
    [...coachKeys.all(accountId), 'organizations'] as const,

  organizationList: (accountId: string) =>
    [...coachKeys.organizations(accountId), 'list'] as const,

  teams: (accountId: string) => [...coachKeys.all(accountId), 'teams'] as const,

  teamList: (accountId: string, organizationId: string) =>
    [...coachKeys.teams(accountId), 'list', organizationId] as const,

  rosters: (accountId: string) => [...coachKeys.all(accountId), 'roster'] as const,

  roster: (accountId: string, teamId: string) =>
    [...coachKeys.rosters(accountId), teamId] as const,

  overviews: (accountId: string) => [...coachKeys.all(accountId), 'overview'] as const,

  overview: (
    accountId: string,
    teamId: string,
    athleteId: string,
    date: DateOnly | null,
  ) => [...coachKeys.overviews(accountId), teamId, athleteId, date] as const,
};
