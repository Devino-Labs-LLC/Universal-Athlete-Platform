import {
  buildCookieHeader,
  createInMemoryCookieStoreForTests,
  getXsrfToken,
  hasRefreshableSessionCookies,
  resolveCookieManagerModule,
  sessionCookiePresence,
} from '@/src/core/api/cookieStore';

describe('cookieStore', () => {
  it('persists Set-Cookie pairs and exposes CSRF token', async () => {
    const store = createInMemoryCookieStoreForTests();
    await store.setFromResponse('http://127.0.0.1:8080', [
      'uap_at=access; Path=/api; HttpOnly',
      'XSRF-TOKEN=csrf-value; Path=/',
    ]);

    const cookies = await store.getCookies('http://127.0.0.1:8080/api/v1/identity/me');
    expect(cookies.uap_at).toBe('access');
    expect(getXsrfToken(cookies)).toBe('csrf-value');
    expect(buildCookieHeader(cookies)).toContain('uap_at=access');
    expect(buildCookieHeader(cookies)).toContain('XSRF-TOKEN=csrf-value');
  });

  it('clears session auth cookies without leaving CSRF/access behind', async () => {
    const store = createInMemoryCookieStoreForTests();
    store.setCookie('http://127.0.0.1:8080', 'uap_at', 'a');
    store.setCookie('http://127.0.0.1:8080', 'uap_rt', 'r');
    store.setCookie('http://127.0.0.1:8080', 'XSRF-TOKEN', 'x');
    store.setCookie('http://127.0.0.1:8080', 'other', 'keep');

    await store.clearSession('http://127.0.0.1:8080');
    const cookies = await store.getCookies('http://127.0.0.1:8080');
    expect(cookies.uap_at).toBeUndefined();
    expect(cookies.uap_rt).toBeUndefined();
    expect(cookies['XSRF-TOKEN']).toBeUndefined();
    expect(cookies.other).toBe('keep');
  });

  it('reports session cookie presence without exposing values', () => {
    const presence = sessionCookiePresence({
      uap_at: 'secret-access',
      uap_rt: 'secret-refresh',
      'XSRF-TOKEN': 'secret-xsrf',
    });
    expect(presence).toEqual({
      access: true,
      refresh: true,
      antiForgery: true,
    });
    expect(JSON.stringify(presence)).not.toContain('secret');
    expect(hasRefreshableSessionCookies({})).toBe(false);
    expect(hasRefreshableSessionCookies({ uap_rt: 'x' })).toBe(true);
  });

  it('resolves CJS cookie manager exports without requiring .default', () => {
    const cjsShape = {
      get: jest.fn(),
      clearAll: jest.fn(),
      setFromResponse: jest.fn(),
      clearByName: jest.fn(),
    };
    expect(resolveCookieManagerModule(cjsShape)).toBe(cjsShape);
    expect(resolveCookieManagerModule({ default: cjsShape })).toBe(cjsShape);
    expect(() => resolveCookieManagerModule(undefined)).toThrow(/unavailable/);
  });
});
