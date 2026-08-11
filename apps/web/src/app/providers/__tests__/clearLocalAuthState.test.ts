import { QueryClient } from '@tanstack/react-query';
import { describe, expect, it, vi } from 'vitest';

import { clearLocalAuthState } from '@/core/auth/clearLocalAuthState';
import type { MeResponse } from '@/features/auth/schemas';
import { athleteQueryKeys } from '@/features/profile/queryKeys';
import { trainingClientKeys } from '@/features/home/queryKeys';

describe('clearLocalAuthState', () => {
  it('clears query cache and auth state', async () => {
    const queryClient = new QueryClient();
    let account: MeResponse | null = {
      accountId: 'acc-1',
      email: 'athlete@example.com',
      status: 'ACTIVE',
      emailVerifiedAt: null,
    };
    let status = 'AUTHENTICATED';

    queryClient.setQueryData(trainingClientKeys.today('2026-08-11' as never), {
      date: '2026-08-11',
    });
    queryClient.setQueryData(athleteQueryKeys.profile(), { id: 'p1' });
    queryClient.setQueryData(athleteQueryKeys.sports(), []);
    queryClient.setQueryData(athleteQueryKeys.goals(), []);

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
    expect(queryClient.getQueryData(trainingClientKeys.today('2026-08-11' as never))).toBeUndefined();
    expect(queryClient.getQueryData(athleteQueryKeys.profile())).toBeUndefined();
    expect(queryClient.getQueryData(athleteQueryKeys.sports())).toBeUndefined();
    expect(queryClient.getQueryData(athleteQueryKeys.goals())).toBeUndefined();
  });

  it('cancels in-flight queries before clear to avoid cross-account cache races', async () => {
    const queryClient = new QueryClient();
    const cancelSpy = vi.spyOn(queryClient, 'cancelQueries');
    const clearSpy = vi.spyOn(queryClient, 'clear');

    await clearLocalAuthState({
      queryClient,
      setAccount: () => undefined,
      setStatus: () => undefined,
    });

    expect(cancelSpy).toHaveBeenCalled();
    expect(clearSpy).toHaveBeenCalled();
    expect(cancelSpy.mock.invocationCallOrder[0]!).toBeLessThan(clearSpy.mock.invocationCallOrder[0]!);
  });
});
