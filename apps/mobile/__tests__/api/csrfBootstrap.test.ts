import MockAdapter from 'axios-mock-adapter';

import { createApiClient } from '@/src/core/api/apiClient';
import {
  CookieStore,
  sessionCookiePresence,
  sessionCookieProbeUrl,
} from '@/src/core/api/cookieStore';
import { beginConnect } from '@/src/features/connectedApps/api/connectionsApi';

/**
 * Simulates Android CookieManager path matching, including the observed quirk where
 * XSRF-TOKEN is visible under /api/v1/identity/* but not under /api/v1/integrations/*.
 */
function createPathAwareCookieStore(): CookieStore & {
  jar: Map<string, { value: string; path: string }>;
  setRaw: (name: string, value: string, path: string) => void;
} {
  const jar = new Map<string, { value: string; path: string }>();

  const pathMatches = (cookiePath: string, requestPath: string): boolean => {
    if (requestPath === cookiePath) return true;
    if (cookiePath === '/') return true;
    const normalized = cookiePath.endsWith('/') ? cookiePath : `${cookiePath}/`;
    return requestPath.startsWith(normalized) || requestPath.startsWith(cookiePath);
  };

  return {
    jar,
    setRaw(name: string, value: string, path: string) {
      jar.set(name, { value, path });
    },
    async getCookies(url: string): Promise<Record<string, string>> {
      const path = new URL(url).pathname || '/';
      const out: Record<string, string> = {};
      for (const [name, entry] of jar.entries()) {
        if (pathMatches(entry.path, path)) {
          out[name] = entry.value;
        }
      }
      return out;
    },
    async setFromResponse(url: string, setCookieHeader: string | string[] | undefined) {
      if (!setCookieHeader) return;
      const headers = Array.isArray(setCookieHeader) ? setCookieHeader : [setCookieHeader];
      for (const header of headers) {
        const [pair, ...attrs] = header.split(';').map((p) => p.trim());
        const eq = pair.indexOf('=');
        if (eq <= 0) continue;
        const name = pair.slice(0, eq);
        const value = pair.slice(eq + 1);
        let path = '/';
        for (const attr of attrs) {
          if (attr.toLowerCase().startsWith('path=')) {
            path = attr.slice(5);
          }
        }
        jar.set(name, { value, path });
      }
    },
    async ensureCsrfCookie(_apiBaseUrl: string, token: string) {
      jar.set('XSRF-TOKEN', { value: token, path: '/' });
    },
    async clearSession() {
      jar.clear();
    },
    async clearAll() {
      jar.clear();
    },
  };
}

describe('CSRF bootstrap for protected writes', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('attaches X-XSRF-TOKEN when request-scoped cookies already include XSRF', async () => {
    const store = createPathAwareCookieStore();
    store.setRaw('uap_at', 'access', '/api');
    store.setRaw('XSRF-TOKEN', 'xsrf-present', '/');
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    let seenHeader: string | undefined;
    let seenCookie: string | undefined;
    mock.onPost('/api/v1/integrations/connections').reply((config) => {
      seenHeader = String(config.headers?.['X-XSRF-TOKEN'] ?? '');
      seenCookie = String(config.headers?.Cookie ?? '');
      return [200, { connectionId: 'c1', provider: 'HEALTH_CONNECT', lifecycleState: 'PENDING' }];
    });

    await client.axios.post('/api/v1/integrations/connections', {
      requestId: '11111111-1111-4111-8111-111111111111',
      provider: 'HEALTH_CONNECT',
    });

    expect(seenHeader).toBe('xsrf-present');
    expect(seenCookie).toContain('XSRF-TOKEN=');
    expect(seenCookie).not.toContain('xsrf-present-value-leak-check');
    mock.restore();
  });

  it('probes identity jar and attaches CSRF when integrations path omits XSRF', async () => {
    const store = createPathAwareCookieStore();
    // Reproduce device: access visible on integrations; XSRF only under identity path.
    store.setRaw('uap_at', 'access', '/api');
    store.setRaw('uap_rt', 'refresh', '/api/v1/identity');
    store.setRaw('XSRF-TOKEN', 'xsrf-identity-scoped', '/api/v1/identity');

    const atIntegrations = await store.getCookies(
      'http://127.0.0.1:8080/api/v1/integrations/connections',
    );
    expect(sessionCookiePresence(atIntegrations)).toEqual({
      access: true,
      refresh: false,
      antiForgery: false,
    });
    expect(sessionCookiePresence(await store.getCookies(sessionCookieProbeUrl('http://127.0.0.1:8080')))).toEqual({
      access: true,
      refresh: true,
      antiForgery: true,
    });

    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    let meCalls = 0;
    mock.onGet('/api/v1/identity/me').reply(() => {
      meCalls += 1;
      return [200, { accountId: 'a1', email: 'a@example.com', status: 'ACTIVE' }];
    });
    let seenHeader: string | undefined;
    mock.onPost('/api/v1/integrations/connections').reply((config) => {
      seenHeader = String(config.headers?.['X-XSRF-TOKEN'] ?? '');
      const cookie = String(config.headers?.Cookie ?? '');
      expect(cookie).toContain('uap_at=');
      expect(cookie).toContain('XSRF-TOKEN=');
      return [
        200,
        {
          connectionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
          provider: 'HEALTH_CONNECT',
          lifecycleState: 'PENDING',
          processConsentGranted: false,
          processConsentGrantedAt: null,
          connectedAt: null,
          disconnectedAt: null,
          lastSuccessfulSyncAt: null,
          lastAttemptedSyncAt: null,
        },
      ];
    });

    const pending = await beginConnect(client, {
      requestId: '22222222-2222-4222-8222-222222222222',
      provider: 'HEALTH_CONNECT',
    });

    expect(pending.connectionId).toBe('aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee');
    expect(seenHeader).toBe('xsrf-identity-scoped');
    // Probe hit is enough — no seed churn when token already in identity jar.
    expect(meCalls).toBe(0);
    // ensureCsrfCookie promotes Path=/ so integrations URL can see XSRF.
    expect(store.jar.get('XSRF-TOKEN')?.path).toBe('/');
    mock.restore();
  });

  it('seeds GET /identity/me when authenticated but XSRF missing, then attaches token', async () => {
    const store = createPathAwareCookieStore();
    store.setRaw('uap_at', 'access', '/api');
    store.setRaw('uap_rt', 'refresh', '/api/v1/identity');

    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    let meCalls = 0;
    mock.onGet('/api/v1/identity/me').reply(() => {
      meCalls += 1;
      store.setRaw('XSRF-TOKEN', 'xsrf-seeded', '/');
      return [
        200,
        {
          accountId: 'a1',
          email: 'a@example.com',
          status: 'ACTIVE',
          emailVerifiedAt: null,
        },
      ];
    });
    let seenHeader: string | undefined;
    mock.onPost('/api/v1/identity/logout').reply((config) => {
      seenHeader = String(config.headers?.['X-XSRF-TOKEN'] ?? '');
      return [204];
    });

    await client.axios.post('/api/v1/identity/logout');
    expect(meCalls).toBe(1);
    expect(seenHeader).toBe('xsrf-seeded');
    mock.restore();
  });

  it('does not send protected mutation when seed fails to produce XSRF', async () => {
    const store = createPathAwareCookieStore();
    store.setRaw('uap_at', 'access', '/api');

    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    mock.onGet('/api/v1/identity/me').reply(200, {
      accountId: 'a1',
      email: 'a@example.com',
      status: 'ACTIVE',
      emailVerifiedAt: null,
    });
    let postCalls = 0;
    mock.onPost('/api/v1/integrations/connections').reply(() => {
      postCalls += 1;
      return [200, {}];
    });

    await expect(
      client.axios.post('/api/v1/integrations/connections', {
        requestId: '33333333-3333-4333-8333-333333333333',
        provider: 'HEALTH_CONNECT',
      }),
    ).rejects.toMatchObject({
      name: 'ApiError',
      code: 'CSRF_TOKEN_UNAVAILABLE',
    });
    expect(postCalls).toBe(0);
    mock.restore();
  });

  it('does not CSRF-seed when there are no authenticated cookies', async () => {
    const store = createPathAwareCookieStore();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    let meCalls = 0;
    mock.onGet('/api/v1/identity/me').reply(() => {
      meCalls += 1;
      return [401];
    });
    mock.onPost('/api/v1/integrations/connections').reply(401, {
      code: 'UNAUTHENTICATED',
      message: 'Authentication is required',
    });

    await expect(
      client.axios.post('/api/v1/integrations/connections', {
        requestId: '44444444-4444-4444-8444-444444444444',
        provider: 'HEALTH_CONNECT',
      }),
    ).rejects.toMatchObject({ status: 401 });

    expect(meCalls).toBe(0);
    mock.restore();
  });

  it('logs CSRF attachment presence without token values', async () => {
    const store = createPathAwareCookieStore();
    store.setRaw('uap_at', 'secret-access', '/api');
    store.setRaw('XSRF-TOKEN', 'secret-xsrf-value', '/api/v1/identity');
    const debugSpy = jest.spyOn(console, 'debug').mockImplementation(() => undefined);

    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    mock.onPost('/api/v1/identity/logout').reply(204);

    await client.axios.post('/api/v1/identity/logout');

    const csrfLog = debugSpy.mock.calls.find((call) =>
      String(call[0]).includes('CSRF-protected request header attachment'),
    );
    expect(csrfLog).toBeDefined();
    const payload = JSON.stringify(csrfLog?.[1] ?? {});
    expect(payload).toContain('"antiForgeryHeader":"attached"');
    expect(payload).not.toContain('secret-xsrf-value');
    expect(payload).not.toContain('secret-access');
    mock.restore();
  });
});
