import MockAdapter from 'axios-mock-adapter';

import { createApiClient } from '@/src/core/api/apiClient';
import { createInMemoryCookieStoreForTests } from '@/src/core/api/cookieStore';
import { fetchMe, login, logout } from '@/src/features/auth/api';

describe('auth session api', () => {
  it('loads the current account', async () => {
    const cookieStore = createInMemoryCookieStoreForTests();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore,
    });
    const mock = new MockAdapter(client.axios);

    mock.onGet('/api/v1/identity/me').reply(200, {
      accountId: 'acc-1',
      email: 'athlete@example.com',
      status: 'ACTIVE',
      emailVerifiedAt: '2026-01-01T00:00:00Z',
    });

    const me = await fetchMe(client);
    expect(me.email).toBe('athlete@example.com');
    mock.restore();
  });

  it('logs in then fetches me', async () => {
    const cookieStore = createInMemoryCookieStoreForTests();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore,
    });
    const mock = new MockAdapter(client.axios);

    mock.onPost('/api/v1/identity/login').reply(200, {
      accountId: 'acc-1',
      status: 'ACTIVE',
    });
    mock.onGet('/api/v1/identity/me').reply(200, {
      accountId: 'acc-1',
      email: 'athlete@example.com',
      status: 'ACTIVE',
      emailVerifiedAt: null,
    });

    const me = await login(client, {
      email: 'athlete@example.com',
      password: 'secret123',
    });

    expect(me.accountId).toBe('acc-1');
    mock.restore();
  });

  it('posts logout with CSRF when cookie is present', async () => {
    const cookieStore = createInMemoryCookieStoreForTests();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore,
    });
    const mock = new MockAdapter(client.axios);

    await cookieStore.setCookie('http://127.0.0.1:8080', 'XSRF-TOKEN', 'csrf-token');

    mock.onPost('/api/v1/identity/logout').reply((config) => {
      expect(config.headers?.['X-XSRF-TOKEN']).toBe('csrf-token');
      return [204];
    });

    await logout(client);
    mock.restore();
  });
});
