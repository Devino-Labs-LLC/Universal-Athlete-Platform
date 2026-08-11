import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it } from 'vitest';

import { clearLocalAuthState } from '@/core/auth/clearLocalAuthState';
import { athleteQueryKeys } from '@/features/profile/queryKeys';
import { trainingClientKeys } from '@/features/home/queryKeys';

// Distinct from RC04 (cross-account isolation across two logins): this
// asserts the *entire* cache is empty after logout, so no protected data of
// any shape — training, profile, or otherwise — remains readable by any
// consumer that might mount after the auth state flips.
describe('RC20 — logout clears the cache so no protected data remains readable', () => {
  it('empties the whole query cache, not just a known subset of keys', async () => {
    const queryClient = new QueryClient();

    queryClient.setQueryData(trainingClientKeys.today('2026-08-11' as never), { date: '2026-08-11' });
    queryClient.setQueryData(athleteQueryKeys.profile(), { id: 'athlete-1' });
    queryClient.setQueryData(athleteQueryKeys.sports(), [{ id: 'sport-1' }]);
    queryClient.setQueryData(athleteQueryKeys.goals(), [{ id: 'goal-1' }]);
    queryClient.setQueryData(['some-future-protected-feature', 'detail', 'x'], { secret: true });

    expect(queryClient.getQueryCache().getAll().length).toBeGreaterThan(0);

    let account: unknown = { accountId: 'acc-1' };
    let status = 'AUTHENTICATED';

    await clearLocalAuthState({
      queryClient,
      setAccount: (next) => {
        account = next;
      },
      setStatus: (next) => {
        status = next;
      },
    });

    expect(account).toBeNull();
    expect(status).toBe('UNAUTHENTICATED');
    expect(queryClient.getQueryCache().getAll()).toHaveLength(0);
  });
});
