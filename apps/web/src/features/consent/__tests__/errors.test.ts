import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import { consentErrorMessage } from '@/features/consent/models/errors';

describe('consentErrorMessage', () => {
  it('maps known consent codes', () => {
    expect(
      consentErrorMessage(
        new ApiError('backend', {
          category: 'CONFLICT',
          status: 409,
          code: 'ACTIVE_GRANT_EXISTS',
        }),
      ),
    ).toMatch(/already have an active sharing grant/i);

    expect(
      consentErrorMessage(
        new ApiError('backend', { category: 'NOT_FOUND', status: 404, code: 'CONSENT_NOT_FOUND' }),
      ),
    ).toMatch(/not found/i);

    expect(
      consentErrorMessage(
        new ApiError('backend message', { category: 'SERVER', status: 500, code: 'UNKNOWN_CODE' }),
      ),
    ).toBe('backend message');
  });

  it('falls back for unknown errors', () => {
    expect(consentErrorMessage(new Error('boom'), 'fallback')).toBe('boom');
    expect(consentErrorMessage({}, 'fallback')).toBe('fallback');
  });
});
