import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  apiHostForDisplay,
  formatLoginFailure,
  identityErrorMessage,
} from '@/features/auth/errorMessages';

describe('identityErrorMessage', () => {
  it('explains CORS/network blocks without leaking secrets', () => {
    const message = identityErrorMessage(
      new ApiError('Network Error', { category: 'NETWORK' }),
    );

    expect(message).toMatch(/CORS/);
    expect(message).not.toMatch(/uap_at=|password|jwt|XSRF/i);
  });
});

describe('formatLoginFailure', () => {
  it('includes category, status, and API host only', () => {
    const text = formatLoginFailure(
      new ApiError('Unauthorized', {
        category: 'UNAUTHORIZED',
        status: 401,
        code: 'INVALID_CREDENTIALS',
      }),
      'https://uapserver-production.up.railway.app',
    );

    expect(text).toContain('Email or password is incorrect.');
    expect(text).toContain('UNAUTHORIZED');
    expect(text).toContain('401');
    expect(text).toContain('uapserver-production.up.railway.app');
    expect(text).not.toMatch(/uap_at=|Bearer |csrf/i);
  });

  it('does not fall back to localhost for a production API host', () => {
    expect(apiHostForDisplay('https://uapserver-production.up.railway.app')).toBe(
      'uapserver-production.up.railway.app',
    );
    expect(apiHostForDisplay('https://uapserver-production.up.railway.app')).not.toMatch(
      /127\.0\.0\.1|localhost/,
    );
  });
});
