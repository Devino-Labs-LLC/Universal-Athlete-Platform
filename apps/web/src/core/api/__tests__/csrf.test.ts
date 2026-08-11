import { describe, expect, it } from 'vitest';

import {
  __setCsrfTokenForTests,
  buildCsrfHeader,
  isCsrfExemptPath,
  readCsrfToken,
  shouldAttachCsrf,
} from '@/core/api/csrf';

describe('csrf', () => {
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

  it('reads token from memory store when cookie is unavailable', () => {
    __setCsrfTokenForTests('memory-token');
    expect(readCsrfToken()).toBe('memory-token');
    __setCsrfTokenForTests(null);
  });
});
