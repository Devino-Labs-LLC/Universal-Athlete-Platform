import MockAdapter from 'axios-mock-adapter';

import { createApiClient } from '@/src/core/api/apiClient';
import { createInMemoryCookieStoreForTests } from '@/src/core/api/cookieStore';
import { isUnauthorizedError } from '@/src/core/api/errorMapper';
import { ApiError } from '@/src/core/api/errors';
import { fetchMe } from '@/src/features/auth/api';

describe('fresh-install restore semantics via identity/me', () => {
  it('maps clean-install 401 to unauthorized without inventing network failure', async () => {
    const cookieStore = createInMemoryCookieStoreForTests();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore,
    });
    const mock = new MockAdapter(client.axios);

    mock.onGet('/api/v1/identity/me').reply(401, {
      message: 'Authentication is required',
      code: 'UNAUTHENTICATED',
      path: '/api/v1/identity/me',
    });

    let caught: unknown;
    try {
      await fetchMe(client);
    } catch (error) {
      caught = error;
    }

    expect(caught).toBeInstanceOf(ApiError);
    expect(isUnauthorizedError(caught)).toBe(true);
    expect((caught as ApiError).status).toBe(401);
    expect((caught as ApiError).category).toBe('unauthorized');
    // No refresh attempt when session cookies are absent.
    expect(mock.history.post?.some((r) => r.url?.includes('/identity/refresh'))).toBeFalsy();
    mock.restore();
  });

  it('keeps genuine network failures as network ApiError (not unauthorized)', async () => {
    const cookieStore = createInMemoryCookieStoreForTests();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore,
    });
    const mock = new MockAdapter(client.axios);
    mock.onGet('/api/v1/identity/me').networkError();

    let caught: unknown;
    try {
      await fetchMe(client);
    } catch (error) {
      caught = error;
    }

    expect(caught).toBeInstanceOf(ApiError);
    expect((caught as ApiError).category).toBe('network');
    expect(isUnauthorizedError(caught)).toBe(false);
    mock.restore();
  });

  it('continues the request when cookie getCookies throws (interop failure path)', async () => {
    const cookieStore = createInMemoryCookieStoreForTests();
    cookieStore.getCookies = async () => {
      throw new TypeError("Cannot read properties of undefined (reading 'get')");
    };

    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore,
    });
    const mock = new MockAdapter(client.axios);
    mock.onGet('/api/v1/identity/me').reply(401, {
      message: 'Authentication is required',
      code: 'UNAUTHENTICATED',
    });

    let caught: unknown;
    try {
      await fetchMe(client);
    } catch (error) {
      caught = error;
    }

    expect(isUnauthorizedError(caught)).toBe(true);
    mock.restore();
  });
});
