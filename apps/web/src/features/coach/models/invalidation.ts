import type { QueryClient } from '@tanstack/react-query';

import { coachKeys } from '@/features/coach/models/queryKeys';

/** Remove every coach cache entry (all accounts). Prefer on logout / 401. */
export function clearAllCoachQueries(queryClient: QueryClient): void {
  void queryClient.removeQueries({ queryKey: coachKeys.root });
}

/** Remove coach cache for one account (persona switch, account switch). */
export function clearCoachAccountQueries(
  queryClient: QueryClient,
  accountId: string,
): void {
  void queryClient.removeQueries({ queryKey: coachKeys.all(accountId) });
}

/** Drop roster + overview for a team so Team A data cannot flash after Team B select. */
export function clearCoachTeamQueries(
  queryClient: QueryClient,
  accountId: string,
  teamId: string,
): void {
  void queryClient.removeQueries({ queryKey: coachKeys.roster(accountId, teamId) });
  void queryClient.removeQueries({
    queryKey: [...coachKeys.overviews(accountId), teamId],
  });
  void queryClient.removeQueries({
    queryKey: [...coachKeys.all(accountId), 'assignments', teamId],
  });
  void queryClient.removeQueries({
    queryKey: [...coachKeys.all(accountId), 'team-readiness', teamId],
  });
}

export function clearCoachRosterQuery(
  queryClient: QueryClient,
  accountId: string,
  teamId: string,
): void {
  void queryClient.removeQueries({ queryKey: coachKeys.roster(accountId, teamId) });
}

export function clearCoachOverviewQueries(
  queryClient: QueryClient,
  accountId: string,
  teamId: string,
  athleteId: string,
): void {
  void queryClient.removeQueries({
    queryKey: [...coachKeys.overviews(accountId), teamId, athleteId],
  });
}

/** Persona switch athlete ↔ coach: hard-clear coach projections for this account. */
export function clearCoachQueriesOnPersonaSwitch(
  queryClient: QueryClient,
  accountId: string | null | undefined,
): void {
  if (accountId) {
    clearCoachAccountQueries(queryClient, accountId);
  } else {
    clearAllCoachQueries(queryClient);
  }
}
