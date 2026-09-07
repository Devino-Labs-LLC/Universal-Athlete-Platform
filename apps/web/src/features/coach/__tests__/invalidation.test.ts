import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it } from 'vitest';

import { parseDateOnly } from '@/core/date/dateOnly';
import {
  clearAllCoachQueries,
  clearCoachAccountQueries,
  clearCoachOverviewQueries,
  clearCoachQueriesOnPersonaSwitch,
  clearCoachRosterQuery,
  clearCoachTeamQueries,
} from '@/features/coach/models/invalidation';
import { coachKeys } from '@/features/coach/models/queryKeys';

describe('coach invalidation', () => {
  it('clears all coach queries under the root key', () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-a'), [{ athleteId: 'a1' }]);
    queryClient.setQueryData(coachKeys.roster('acc-2', 'team-b'), [{ athleteId: 'b1' }]);

    clearAllCoachQueries(queryClient);

    expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-a'))).toBeUndefined();
    expect(queryClient.getQueryData(coachKeys.roster('acc-2', 'team-b'))).toBeUndefined();
  });

  it('clears one account without touching another', () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-a'), [{ athleteId: 'a1' }]);
    queryClient.setQueryData(coachKeys.roster('acc-2', 'team-b'), [{ athleteId: 'b1' }]);

    clearCoachAccountQueries(queryClient, 'acc-1');

    expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-a'))).toBeUndefined();
    expect(queryClient.getQueryData(coachKeys.roster('acc-2', 'team-b'))).toEqual([
      { athleteId: 'b1' },
    ]);
  });

  it('clears team-scoped roster and overview so Team A cannot flash after Team B', () => {
    const queryClient = new QueryClient();
    const date = parseDateOnly('2026-09-07');
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-a'), [
      { displayName: 'Team A athlete' },
    ]);
    queryClient.setQueryData(coachKeys.overview('acc-1', 'team-a', 'ath-1', date), {
      displayName: 'Team A athlete',
    });
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-b'), [
      { displayName: 'Team B athlete' },
    ]);

    clearCoachTeamQueries(queryClient, 'acc-1', 'team-a');

    expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-a'))).toBeUndefined();
    expect(
      queryClient.getQueryData(coachKeys.overview('acc-1', 'team-a', 'ath-1', date)),
    ).toBeUndefined();
    expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-b'))).toEqual([
      { displayName: 'Team B athlete' },
    ]);
  });

  it('clears roster and overview helpers independently', () => {
    const queryClient = new QueryClient();
    const date = parseDateOnly('2026-09-07');
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-a'), [{ athleteId: 'a1' }]);
    queryClient.setQueryData(coachKeys.overview('acc-1', 'team-a', 'ath-1', date), {
      displayName: 'A',
    });

    clearCoachRosterQuery(queryClient, 'acc-1', 'team-a');
    clearCoachOverviewQueries(queryClient, 'acc-1', 'team-a', 'ath-1');

    expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-a'))).toBeUndefined();
    expect(
      queryClient.getQueryData(coachKeys.overview('acc-1', 'team-a', 'ath-1', date)),
    ).toBeUndefined();
  });

  it('persona switch clears the active account coach cache', () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-a'), [{ athleteId: 'a1' }]);

    clearCoachQueriesOnPersonaSwitch(queryClient, 'acc-1');

    expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-a'))).toBeUndefined();
  });

  it('persona switch without account clears the entire coach root', () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-a'), [{ athleteId: 'a1' }]);
    clearCoachQueriesOnPersonaSwitch(queryClient, null);
    expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-a'))).toBeUndefined();
  });
});
