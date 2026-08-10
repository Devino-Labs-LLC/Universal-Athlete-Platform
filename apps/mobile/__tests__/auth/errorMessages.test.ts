import {
  getIdentityErrorMessage,
  identityErrorMessage,
} from '@/src/features/auth/errorMessages';
import { ApiError } from '@/src/core/api/errors';

describe('identity error mapping', () => {
  it('maps known identity codes to stable copy', () => {
    expect(getIdentityErrorMessage('DUPLICATE_EMAIL')).toMatch(/already exists/i);
    expect(getIdentityErrorMessage('EMAIL_NOT_VERIFIED')).toMatch(/verify your email/i);
    expect(getIdentityErrorMessage('INVALID_CREDENTIALS')).toMatch(/incorrect/i);
  });

  it('prefers mapped copy over raw API message', () => {
    const error = new ApiError('raw', {
      category: 'validation',
      code: 'PASSWORD_POLICY_VIOLATION',
    });

    expect(identityErrorMessage(error)).toMatch(/12–128 characters/i);
  });

  it('falls back to API message for unknown codes', () => {
    const error = new ApiError('Something specific', {
      category: 'unknown',
      code: 'UNKNOWN_CODE',
    });

    expect(identityErrorMessage(error)).toBe('Something specific');
  });
});
