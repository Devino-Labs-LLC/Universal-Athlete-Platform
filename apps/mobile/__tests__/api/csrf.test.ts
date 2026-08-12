import MockAdapter from 'axios-mock-adapter';

import { createApiClient } from '@/src/core/api/apiClient';
import { createInMemoryCookieStoreForTests } from '@/src/core/api/cookieStore';
import { buildCsrfHeader, isCsrfExemptPath, shouldAttachCsrf } from '@/src/core/api/csrf';

describe('csrf', () => {
  afterEach(() => {
    jest.restoreAllMocks();
  });

  it('exempts identity bootstrap endpoints', () => {
    expect(isCsrfExemptPath('/api/v1/identity/login')).toBe(true);
    expect(isCsrfExemptPath('/api/v1/identity/register')).toBe(true);
    expect(isCsrfExemptPath('/api/v1/identity/verify-email')).toBe(true);
    expect(isCsrfExemptPath('/api/v1/identity/logout')).toBe(false);
  });

  it('attaches CSRF for mutating non-exempt paths', () => {
    expect(shouldAttachCsrf('POST', '/api/v1/identity/logout')).toBe(true);
    expect(shouldAttachCsrf('GET', '/api/v1/identity/me')).toBe(false);
    expect(shouldAttachCsrf('POST', '/api/v1/identity/login')).toBe(false);
  });

  it('builds the X-XSRF-TOKEN header', () => {
    expect(buildCsrfHeader('abc')).toEqual({ 'X-XSRF-TOKEN': 'abc' });
  });

  it('logs CSRF header attachment as yes/no without token values', async () => {
    const cookieStore = createInMemoryCookieStoreForTests();
    cookieStore.setCookie('http://127.0.0.1:8080', 'XSRF-TOKEN', 'secret-xsrf-value');
    cookieStore.setCookie('http://127.0.0.1:8080', 'uap_at', 'secret-access');
    const debugSpy = jest.spyOn(console, 'debug').mockImplementation(() => undefined);

    const client = createApiClient({
      baseURL: 'http://127.0.0.1:8080',
      cookieStore,
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
    expect(payload).toContain('"access":true');
    expect(payload).toContain('"antiForgery":true');
    expect(payload).not.toContain('secret-xsrf-value');
    expect(payload).not.toContain('secret-access');
    mock.restore();
  });
});
