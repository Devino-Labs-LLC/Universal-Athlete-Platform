import MockAdapter from 'axios-mock-adapter';

import { createApiClient, RefreshMutex } from '@/src/core/api/apiClient';
import { createInMemoryCookieStoreForTests } from '@/src/core/api/cookieStore';

describe('refresh single-flight', () => {
  it('deduplicates concurrent refresh attempts', async () => {
    const cookieStore = createInMemoryCookieStoreForTests();
    let refreshCalls = 0;
    let meCalls = 0;

    const { axios } = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore,
      onSessionExpired: jest.fn(),
    });

    const mock = new MockAdapter(axios);
    mock.onGet('/api/v1/identity/me').reply(() => {
      meCalls += 1;
      if (meCalls <= 2) {
        return [401];
      }
      return [
        200,
        {
          accountId: 'acc-1',
          email: 'athlete@example.com',
          status: 'ACTIVE',
          emailVerifiedAt: null,
        },
      ];
    });
    mock.onPost('/api/v1/identity/refresh').reply(async () => {
      refreshCalls += 1;
      await new Promise((resolve) => setTimeout(resolve, 20));
      return [204];
    });

    const [first, second] = await Promise.all([
      axios.get('/api/v1/identity/me'),
      axios.get('/api/v1/identity/me'),
    ]);

    expect(refreshCalls).toBe(1);
    expect(first.data.accountId).toBe('acc-1');
    expect(second.data.accountId).toBe('acc-1');
    mock.restore();
  });

  it('runs refresh work through the mutex helper', async () => {
    const mutex = new RefreshMutex();
    let runs = 0;

    const [a, b] = await Promise.all([
      mutex.run(async () => {
        runs += 1;
        await new Promise((resolve) => setTimeout(resolve, 10));
        return true;
      }),
      mutex.run(async () => {
        runs += 1;
        return true;
      }),
    ]);

    expect(a).toBe(true);
    expect(b).toBe(true);
    expect(runs).toBe(1);
  });
});
