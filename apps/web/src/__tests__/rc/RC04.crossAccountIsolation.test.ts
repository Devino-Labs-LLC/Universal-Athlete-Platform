import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it } from 'vitest';

import { clearLocalAuthState } from '@/core/auth/clearLocalAuthState';
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
});
