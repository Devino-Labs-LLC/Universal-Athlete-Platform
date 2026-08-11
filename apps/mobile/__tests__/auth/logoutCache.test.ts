import { QueryClient } from '@tanstack/react-query';

import { CookieStore } from '@/src/core/api/cookieStore';
import { clearLocalAuthState } from '@/src/app/providers/AuthSessionProvider';
import { athleteQueryKeys } from '@/src/features/profile/queryKeys';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import { environmentKeys } from '@/src/features/environments/models/environmentKeys';

function createThrowingCookieStore(): CookieStore {
  return {
    getCookies: async () => ({}),
    setFromResponse: async () => undefined,
    clearSession: async () => undefined,
    clearAll: async () => {
      throw new Error('cookie clear failed');
    },
  };
}

function createOkCookieStore(cleared: { value: boolean }): CookieStore {
  return {
    getCookies: async () => ({}),
    setFromResponse: async () => undefined,
    clearSession: async () => undefined,
    clearAll: async () => {
      cleared.value = true;
    },
  };
}

describe('logout / cross-account cache isolation', () => {
  it('clears athlete-specific cached data between accounts', async () => {
    const queryClient = new QueryClient();
    const cookiesCleared = { value: false };
    let account: { id: string } | null = { id: 'athlete-a' };
    let status: string = 'AUTHENTICATED';

    queryClient.setQueryData(athleteQueryKeys.profile(), {
      id: 'athlete-a',
      firstName: 'Athlete',
      lastName: 'A',
    });
    queryClient.setQueryData(todayQueryKeys.date('2026-08-11'), {
      athleteName: 'Athlete A',
    });
    queryClient.setQueryData(environmentKeys.list({ activeOnly: true }), {
      environments: [{ id: 'env-a', name: 'Athlete A Gym' }],
    });

    await clearLocalAuthState({
      queryClient,
      cookieStore: createOkCookieStore(cookiesCleared),
      setAccount: (next) => {
        account = next;
      },
      setStatus: (next) => {
        status = next;
      },
    });

    expect(cookiesCleared.value).toBe(true);
    expect(account).toBeNull();
    expect(status).toBe('UNAUTHENTICATED');
    expect(queryClient.getQueryData(athleteQueryKeys.profile())).toBeUndefined();
    expect(queryClient.getQueryData(todayQueryKeys.date('2026-08-11'))).toBeUndefined();
    expect(
      queryClient.getQueryData(environmentKeys.list({ activeOnly: true })),
    ).toBeUndefined();
  });

  it('still clears query cache and auth state when cookie clear fails', async () => {
    const queryClient = new QueryClient();
    let account: { id: string } | null = { id: 'athlete-a' };
    let status: string = 'AUTHENTICATED';

    queryClient.setQueryData(athleteQueryKeys.profile(), { id: 'athlete-a' });

    await clearLocalAuthState({
      queryClient,
      cookieStore: createThrowingCookieStore(),
      setAccount: (next) => {
        account = next;
      },
      setStatus: (next) => {
        status = next;
      },
    });

    expect(account).toBeNull();
    expect(status).toBe('UNAUTHENTICATED');
    expect(queryClient.getQueryData(athleteQueryKeys.profile())).toBeUndefined();
  });
});
