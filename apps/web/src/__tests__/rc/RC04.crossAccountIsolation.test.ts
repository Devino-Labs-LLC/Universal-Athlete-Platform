import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it } from 'vitest';

import { clearLocalAuthState } from '@/core/auth/clearLocalAuthState';
import { parseDateOnly } from '@/core/date/dateOnly';
import { coachKeys } from '@/features/coach/models/queryKeys';
import { consentKeys } from '@/features/consent/models/queryKeys';
import { trainingClientKeys } from '@/features/home/queryKeys';
import { athleteQueryKeys } from '@/features/profile/queryKeys';

interface CachedProfile {
  id: string;
  firstName: string;
}

describe('RC04 — cross-account cache isolation on the same device/tab', () => {
  it('never lets Athlete A protected data survive into an Athlete B session', async () => {
    const queryClient = new QueryClient();

    // Athlete A signs in; their data populates the shared TanStack Query cache.
    queryClient.setQueryData<CachedProfile>(athleteQueryKeys.profile(), {
      id: 'athlete-a',
      firstName: 'Alice',
    });
    queryClient.setQueryData(trainingClientKeys.today('2026-08-11' as never), {
      athlete: { athleteId: 'athlete-a', displayName: 'Alice' },
    });
    expect(queryClient.getQueryData(athleteQueryKeys.profile())).toBeDefined();

    // Athlete A logs out (or their session is torn down for any reason).
    await clearLocalAuthState({
      queryClient,
      setAccount: () => undefined,
      setStatus: () => undefined,
    });

    expect(queryClient.getQueryData(athleteQueryKeys.profile())).toBeUndefined();
    expect(queryClient.getQueryData(trainingClientKeys.today('2026-08-11' as never))).toBeUndefined();

    // Athlete B logs in on the same browser tab.
    queryClient.setQueryData<CachedProfile>(athleteQueryKeys.profile(), {
      id: 'athlete-b',
      firstName: 'Bob',
    });
    queryClient.setQueryData(trainingClientKeys.today('2026-08-11' as never), {
      athlete: { athleteId: 'athlete-b', displayName: 'Bob' },
    });

    const bProfile = queryClient.getQueryData<CachedProfile>(athleteQueryKeys.profile());
    expect(bProfile?.id).toBe('athlete-b');

    // Athlete A's identity must never resurface under Athlete B's session.
    const allCachedProfiles = queryClient
      .getQueriesData<CachedProfile>({ queryKey: athleteQueryKeys.all })
      .map(([, data]) => data)
      .filter((data): data is CachedProfile => Boolean(data));
    expect(allCachedProfiles.every((profile) => profile.id !== 'athlete-a')).toBe(true);
  });

  it('never lets Account A coach roster/overview survive into Account B', async () => {
    const queryClient = new QueryClient();
    const date = parseDateOnly('2026-09-07');

    queryClient.setQueryData(coachKeys.roster('acc-a', 'team-1'), [
      { athleteId: 'ath-a', displayName: 'Alice Athlete' },
    ]);
    queryClient.setQueryData(coachKeys.overview('acc-a', 'team-1', 'ath-a', date), {
      displayName: 'Alice Athlete',
      readinessScore: { status: 'AVAILABLE', data: { readinessScore: 90 } },
    });

    await clearLocalAuthState({
      queryClient,
      setAccount: () => undefined,
      setStatus: () => undefined,
    });

    expect(queryClient.getQueryData(coachKeys.roster('acc-a', 'team-1'))).toBeUndefined();
    expect(
      queryClient.getQueryData(coachKeys.overview('acc-a', 'team-1', 'ath-a', date)),
    ).toBeUndefined();

    queryClient.setQueryData(coachKeys.roster('acc-b', 'team-2'), [
      { athleteId: 'ath-b', displayName: 'Bob Athlete' },
    ]);

    const leakedA = queryClient
      .getQueriesData({ queryKey: coachKeys.root })
      .flatMap(([, data]) => (Array.isArray(data) ? data : [data]))
      .filter(Boolean);
    expect(
      leakedA.every((entry) => {
        if (entry && typeof entry === 'object' && 'displayName' in entry) {
          return (entry as { displayName?: string }).displayName !== 'Alice Athlete';
        }
        if (entry && typeof entry === 'object' && 'athleteId' in entry) {
          return (entry as { athleteId?: string }).athleteId !== 'ath-a';
        }
        return true;
      }),
    ).toBe(true);
  });

  it('keeps Team A and Team B coach keys isolated for the same account', () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-a'), [
      { displayName: 'Team A athlete' },
    ]);
    queryClient.setQueryData(coachKeys.roster('acc-1', 'team-b'), [
      { displayName: 'Team B athlete' },
    ]);

    expect(queryClient.getQueryData(coachKeys.roster('acc-1', 'team-a'))).not.toEqual(
      queryClient.getQueryData(coachKeys.roster('acc-1', 'team-b')),
    );
    expect(coachKeys.roster('acc-1', 'team-a')).not.toEqual(coachKeys.roster('acc-1', 'team-b'));
  });

  it('clears account-scoped athlete transparency cache on logout', async () => {
    const queryClient = new QueryClient();
    const accountAKey = consentKeys.transparency('acc-a', 0);
    const accountBKey = consentKeys.transparency('acc-b', 0);

    queryClient.setQueryData(accountAKey, {
      events: [{ type: 'TEAM_JOINED', description: 'You joined Varsity.' }],
      page: 0,
      size: 20,
      hasMore: false,
    });
    // Without logout, A and B keys remain distinct (account-scoped isolation).
    queryClient.setQueryData(accountBKey, {
      events: [{ type: 'CONSENT_GRANTED', description: 'B shared readiness.' }],
      page: 0,
      size: 20,
      hasMore: false,
    });
    expect(queryClient.getQueryData(accountAKey)).not.toEqual(queryClient.getQueryData(accountBKey));

    await clearLocalAuthState({
      queryClient,
      setAccount: () => undefined,
      setStatus: () => undefined,
    });

    expect(queryClient.getQueryData(accountAKey)).toBeUndefined();
    expect(queryClient.getQueryData(accountBKey)).toBeUndefined();

    queryClient.setQueryData(accountBKey, {
      events: [],
      page: 0,
      size: 20,
      hasMore: false,
    });
    expect(queryClient.getQueryData(accountAKey)).toBeUndefined();
    expect(consentKeys.transparency('acc-a', 0)).not.toEqual(consentKeys.transparency('acc-b', 0));
  });
});
