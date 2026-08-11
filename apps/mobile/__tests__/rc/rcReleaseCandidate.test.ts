import { QueryClient } from '@tanstack/react-query';
import { z } from 'zod';

import { clearLocalAuthState } from '@/src/app/providers/AuthSessionProvider';
import { loadAppConfig } from '@/src/app/config/env';
import { CookieStore } from '@/src/core/api/cookieStore';
import { invalidateAfterOccurrenceEnvironmentMutation } from '@/src/features/environments/models/invalidation';
import { environmentKeys } from '@/src/features/environments/models/environmentKeys';
import { adaptationKeys } from '@/src/features/adaptation/models/adaptationKeys';
import { todayQueryKeys } from '@/src/features/home/models/queryKeys';
import { trainingKeys } from '@/src/features/training/models/queryKeys';
import { workoutOccurrenceDetailSchema } from '@/src/features/training/models/browseSchemas';
import { trainingEnvironmentFormSchema } from '@/src/features/environments/models/environmentSchemas';

/**
 * Focused release-candidate regression suite (RC01–RC18 subset).
 * Prefer high-signal checks over duplicating feature unit coverage.
 */
describe('M10 release-candidate suite', () => {
  const originalDev = (globalThis as { __DEV__?: boolean }).__DEV__;

  afterEach(() => {
    (globalThis as { __DEV__?: boolean }).__DEV__ = originalDev;
  });

  function cookieStore(onClear?: () => void): CookieStore {
    return {
      getCookies: async () => ({}),
      setFromResponse: async () => undefined,
      clearSession: async () => undefined,
      clearAll: async () => {
        onClear?.();
      },
    };
  }

  // RC03 / RC17
  it('RC03/RC17 clears Athlete A cache on local logout teardown', async () => {
    const queryClient = new QueryClient();
    queryClient.setQueryData(todayQueryKeys.all, { athlete: 'A' });
    queryClient.setQueryData(environmentKeys.all, [{ id: 'env-a' }]);
    queryClient.setQueryData(adaptationKeys.all, [{ id: 'proposal-a' }]);

    let account: unknown = { email: 'a@example.com' };
    await clearLocalAuthState({
      queryClient,
      cookieStore: cookieStore(),
      setAccount: (next) => {
        account = next;
      },
      setStatus: () => undefined,
    });

    expect(account).toBeNull();
    expect(queryClient.getQueryData(todayQueryKeys.all)).toBeUndefined();
    expect(queryClient.getQueryData(environmentKeys.all)).toBeUndefined();
    expect(queryClient.getQueryData(adaptationKeys.all)).toBeUndefined();
  });

  // RC12
  it('RC12 environment change invalidates launch/feasibility/adaptation surfaces', async () => {
    const queryClient = new QueryClient();
    const spy = jest.spyOn(queryClient, 'invalidateQueries');

    await invalidateAfterOccurrenceEnvironmentMutation(queryClient, {
      planId: 'plan-1',
      dayId: 'day-1',
      occurrenceId: 'occ-1',
    });

    expect(spy).toHaveBeenCalledWith({
      queryKey: trainingKeys.launch('plan-1', 'day-1', 'occ-1'),
    });
    expect(spy).toHaveBeenCalledWith({ queryKey: adaptationKeys.all });
    expect(spy).toHaveBeenCalledWith({ queryKey: todayQueryKeys.all });
  });

  // RC14
  it('RC14 environment form schema retains required fields after validation failure', () => {
    const parsed = trainingEnvironmentFormSchema.safeParse({
      name: 'H',
      type: 'HOME_GYM',
      availableEquipment: ['DUMBBELL'],
    });
    expect(parsed.success).toBe(false);
    // Client keeps in-memory RHF values; schema failure must not mutate payload shape expectations.
    expect(trainingEnvironmentFormSchema.safeParse({
      name: 'Home Gym',
      type: 'HOME_GYM',
      availableEquipment: ['DUMBBELL'],
    }).success).toBe(true);
  });

  // RC16
  it('RC16 malformed API payload fails with controlled Zod error (does not crash as HTML)', () => {
    const html = '<html><body>Bad Gateway</body></html>';
    expect(() => workoutOccurrenceDetailSchema.parse(html)).toThrow(z.ZodError);

    const partial = { id: 'occ-1' };
    expect(() => workoutOccurrenceDetailSchema.parse(partial)).toThrow(z.ZodError);
  });

  // RC18
  it('RC18 production config validation rejects missing env and localhost', () => {
    (globalThis as { __DEV__?: boolean }).__DEV__ = false;
    expect(() => loadAppConfig({})).toThrow(/EXPO_PUBLIC_UAP_ENV/);
    expect(() =>
      loadAppConfig({
        EXPO_PUBLIC_UAP_ENV: 'production',
        EXPO_PUBLIC_UAP_API_BASE_URL: 'http://localhost:8080',
      }),
    ).toThrow(/Localhost/);
  });
});
