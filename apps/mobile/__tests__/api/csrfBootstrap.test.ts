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

  it('does not fabricate a CSRF token when the identity round-trip is unauthenticated', async () => {
    const store = createPathAwareCookieStore();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    let meCalls = 0;
    mock.onGet('/api/v1/identity/me').reply(() => {
      meCalls += 1;
      return [401, { code: 'UNAUTHENTICATED', message: 'Authentication is required' }];
    });
    let seenHeader: string | undefined;
    mock.onPost('/api/v1/integrations/connections').reply((config) => {
      seenHeader = config.headers?.['X-XSRF-TOKEN'] as string | undefined;
      return [401, { code: 'UNAUTHENTICATED', message: 'Authentication is required' }];
    });

    await expect(
      client.axios.post('/api/v1/integrations/connections', {
        requestId: '44444444-4444-4444-8444-444444444444',
        provider: 'HEALTH_CONNECT',
      }),
    ).rejects.toMatchObject({ status: 401 });

    expect(meCalls).toBe(1);
    expect(seenHeader).toBeUndefined();
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
    const presenceLog = debugSpy.mock.calls.find((call) =>
      String(call[0]).includes('CSRF jar presence before protected write'),
    );
    if (presenceLog) {
      const presencePayload = JSON.stringify(presenceLog[1] ?? {});
      expect(presencePayload).not.toContain('secret-xsrf-value');
      expect(presencePayload).not.toContain('secret-access');
    }
    mock.restore();
  });

  it('attaches CSRF from an authenticated identity seed when the jar cannot see session cookies', async () => {
    const store = createBlindCookieStore();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    let meCalls = 0;
    mock.onGet('/api/v1/identity/me').reply(() => {
      meCalls += 1;
      return [
        200,
        { accountId: 'a1', email: 'a@example.com', status: 'ACTIVE', emailVerifiedAt: null },
        { 'set-cookie': 'XSRF-TOKEN=seeded-from-me; Path=/' },
      ];
    });
    let seenHeader: string | undefined;
    let seenCookie: string | undefined;
    mock.onPost('/api/v1/integrations/connections').reply((config) => {
      seenHeader = config.headers?.['X-XSRF-TOKEN'] as string | undefined;
      seenCookie = config.headers?.Cookie as string | undefined;
      return [
        201,
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
      requestId: '55555555-5555-4555-8555-555555555555',
      provider: 'HEALTH_CONNECT',
    });

    expect(pending.lifecycleState).toBe('PENDING');
    expect(meCalls).toBe(1);
    expect(seenHeader).toBe('seeded-from-me');
    expect(seenCookie).toBeUndefined();
    expect(store.ensuredToken).toBe('seeded-from-me');
    mock.restore();
  });

  it('shares one identity seed across concurrent protected writes', async () => {
    const store = createBlindCookieStore();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    let meCalls = 0;
    let releaseSeed: () => void = () => undefined;
    const seedGate = new Promise<void>((resolve) => {
      releaseSeed = resolve;
    });
    mock.onGet('/api/v1/identity/me').reply(async () => {
      meCalls += 1;
      await seedGate;
      return [200, { accountId: 'a1' }, { 'set-cookie': 'XSRF-TOKEN=shared-seed; Path=/' }];
    });
    mock.onPost('/api/v1/identity/logout').reply(204);

    const both = Promise.all([
      client.axios.post('/api/v1/identity/logout'),
      client.axios.post('/api/v1/identity/logout'),
    ]);
    await Promise.resolve();
    releaseSeed();
    await both;

    expect(meCalls).toBe(1);
    mock.restore();
  });

  it('does not seed from refresh and fails closed without waiting on the seed', async () => {
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
      return [200, { accountId: 'a1' }, { 'set-cookie': 'XSRF-TOKEN=should-not-run; Path=/' }];
    });

    const outcome = await Promise.race([
      client.axios
        .post('/api/v1/identity/refresh')
        .then(() => 'sent')
        .catch((error: { code?: string }) => error.code ?? 'error'),
      new Promise<string>((resolve) => {
        setTimeout(() => resolve('deadlock'), 500);
      }),
    ]);

    expect(outcome).toBe('CSRF_TOKEN_UNAVAILABLE');
    expect(meCalls).toBe(0);
    mock.restore();
  });

  it('does not send the protected write when the csrf bootstrap fails', async () => {
    const store = createBlindCookieStore();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    mock.onGet('/api/v1/identity/me').reply(503, {
      code: 'UNAVAILABLE',
      message: 'Service unavailable',
    });
    let postCalls = 0;
    mock.onPost('/api/v1/integrations/connections').reply(() => {
      postCalls += 1;
      return [201, {}];
    });

    await expect(
      beginConnect(client, {
        requestId: '88888888-8888-4888-8888-888888888888',
        provider: 'HEALTH_CONNECT',
      }),
    ).rejects.toMatchObject({
      name: 'ApiError',
      code: 'CSRF_TOKEN_UNAVAILABLE',
    });
    expect(postCalls).toBe(0);
    expect(store.ensuredToken).toBeNull();
    mock.restore();
  });

  it('fails closed when the identity round-trip is authenticated but no CSRF token is issued', async () => {
    const store = createBlindCookieStore();
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
      return [201, {}];
    });

    await expect(
      beginConnect(client, {
        requestId: '66666666-6666-4666-8666-666666666666',
        provider: 'HEALTH_CONNECT',
      }),
    ).rejects.toMatchObject({
      name: 'ApiError',
      code: 'CSRF_TOKEN_UNAVAILABLE',
    });
    expect(postCalls).toBe(0);
    mock.restore();
  });

  it('reads an origin-scoped CSRF cookie when the request path cannot see it', async () => {
    const store = createOriginOnlyCsrfStore();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);
    let meCalls = 0;
    mock.onGet('/api/v1/identity/me').reply(() => {
      meCalls += 1;
      return [200, { accountId: 'a1' }];
    });
    let seenHeader: string | undefined;
    mock.onPost('/api/v1/integrations/connections').reply((config) => {
      seenHeader = String(config.headers?.['X-XSRF-TOKEN'] ?? '');
      return [
        201,
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

    await beginConnect(client, {
      requestId: '77777777-7777-4777-8777-777777777777',
      provider: 'HEALTH_CONNECT',
    });

    expect(seenHeader).toBe('origin-xsrf');
    expect(meCalls).toBe(0);
    mock.restore();
  });
});

function createBlindCookieStore(): CookieStore & { ensuredToken: string | null } {
  return {
    ensuredToken: null,
    async getCookies() {
      return {};
    },
    async setFromResponse() {
      return undefined;
    },
    async ensureCsrfCookie(_apiBaseUrl: string, token: string) {
      this.ensuredToken = token;
    },
    async clearSession() {
      return undefined;
    },
    async clearAll() {
      return undefined;
    },
  };
}

function createOriginOnlyCsrfStore(): CookieStore {
  return {
    async getCookies(url: string) {
      const path = new URL(url).pathname || '/';
      if (path === '/') {
        return { 'XSRF-TOKEN': 'origin-xsrf' };
      }
      if (path.startsWith('/api')) {
        return { uap_at: 'access' };
      }
      return {};
    },
    async setFromResponse() {
      return undefined;
    },
    async ensureCsrfCookie() {
      return undefined;
    },
    async clearSession() {
      return undefined;
    },
    async clearAll() {
      return undefined;
    },
  };
}
