import MockAdapter from 'axios-mock-adapter';

import { createApiClient } from '@/src/core/api/apiClient';
import {
  CookieStore,
  describeSetCookiePresence,
  resolveCookieRequestUrl,
  sessionCookiePresence,
  sessionCookieProbeUrl,
} from '@/src/core/api/cookieStore';
import { login } from '@/src/features/auth/api';

/**
 * Simulates native CookieManager path matching: Path=/api cookies are invisible
 * when get() is called with origin path `/`.
 */
function createPathAwareCookieStore(): CookieStore & {
  jar: Map<string, { value: string; path: string }>;
} {
  const jar = new Map<string, { value: string; path: string }>();

  const pathMatches = (cookiePath: string, requestPath: string): boolean => {
    if (requestPath === cookiePath) return true;
    if (cookiePath === '/') return true;
    return requestPath.startsWith(cookiePath.endsWith('/') ? cookiePath : `${cookiePath}/`)
      || requestPath.startsWith(cookiePath);
  };

  return {
    jar,
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
    async clearSession() {
      jar.clear();
    },
    async clearAll() {
      jar.clear();
    },
  };
}

describe('native login cookie handoff', () => {
  it('resolves cookie probe URLs under /api so Path=/api cookies are visible', () => {
    expect(sessionCookieProbeUrl('http://127.0.0.1:8080')).toBe(
      'http://127.0.0.1:8080/api/v1/identity/me',
    );
    expect(resolveCookieRequestUrl('http://127.0.0.1:8080', '/api/v1/identity/login')).toBe(
      'http://127.0.0.1:8080/api/v1/identity/login',
    );
    expect(resolveCookieRequestUrl('http://127.0.0.1:8080', undefined)).toBe(
      'http://127.0.0.1:8080/api/v1/identity/me',
    );
  });

  it('describes Set-Cookie presence without values', () => {
    expect(describeSetCookiePresence(undefined)).toEqual({
      setCookieHeaderPresent: false,
      setCookieCount: 0,
    });
    expect(
      describeSetCookiePresence([
        'uap_at=secret; Path=/api; HttpOnly',
        'uap_rt=secret; Path=/api/v1/identity; HttpOnly',
      ]),
    ).toEqual({ setCookieHeaderPresent: true, setCookieCount: 2 });
    const json = JSON.stringify(
      describeSetCookiePresence('uap_at=super-secret-value; Path=/api'),
    );
    expect(json).not.toContain('super-secret-value');
  });

  it('origin-only cookie get misses Path=/api cookies (native regression)', async () => {
    const store = createPathAwareCookieStore();
    await store.setFromResponse('http://127.0.0.1:8080/api/v1/identity/login', [
      'uap_at=access; Path=/api; HttpOnly',
      'uap_rt=refresh; Path=/api/v1/identity; HttpOnly',
      'XSRF-TOKEN=xsrf; Path=/',
    ]);

    const atOrigin = await store.getCookies('http://127.0.0.1:8080');
    expect(sessionCookiePresence(atOrigin)).toEqual({
      access: false,
      refresh: false,
      antiForgery: true,
    });

    const atMe = await store.getCookies(sessionCookieProbeUrl('http://127.0.0.1:8080'));
    expect(sessionCookiePresence(atMe)).toEqual({
      access: true,
      refresh: true,
      antiForgery: true,
    });
  });

  it('login then /me succeeds when cookies are read with request path', async () => {
    const store = createPathAwareCookieStore();
    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore: store,
    });
    const mock = new MockAdapter(client.axios);

    mock.onPost('/api/v1/identity/login').reply(200, { accountId: 'acc-1', status: 'ACTIVE' }, {
      'set-cookie': [
        'uap_at=access-token; Path=/api; HttpOnly; SameSite=Lax',
        'uap_rt=refresh-token; Path=/api/v1/identity; HttpOnly; SameSite=Lax',
        'XSRF-TOKEN=xsrf-token; Path=/; SameSite=Lax',
      ],
    });
    mock.onGet('/api/v1/identity/me').reply((config) => {
      const cookie = String(config.headers?.Cookie ?? '');
      const hasAccess = cookie.includes('uap_at=');
      const hasRefresh = cookie.includes('uap_rt=');
      const hasXsrf = cookie.includes('XSRF-TOKEN=');
      if (!hasAccess) {
        return [401, { code: 'UNAUTHENTICATED', message: 'Authentication is required' }];
      }
      expect(hasRefresh).toBe(true);
      expect(hasXsrf).toBe(true);
      return [
        200,
        {
          accountId: 'acc-1',
          email: 'ra1.user1@devinolabs.test',
          status: 'ACTIVE',
          emailVerifiedAt: '2026-01-01T00:00:00Z',
        },
      ];
    });

    const me = await login(client, {
      email: 'ra1.user1@devinolabs.test',
      password: 'does-not-matter-for-mock',
    });
    expect(me.accountId).toBe('acc-1');
    expect(sessionCookiePresence(await store.getCookies(sessionCookieProbeUrl('http://127.0.0.1:8080')))).toEqual({
      access: true,
      refresh: true,
      antiForgery: true,
    });
    mock.restore();
  });
});
