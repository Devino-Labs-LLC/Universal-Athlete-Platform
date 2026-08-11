import { isApiError } from '@/core/api/errors';

const IDENTITY_ERROR_MESSAGES: Record<string, string> = {
  DUPLICATE_EMAIL: 'An account with this email already exists.',
  PASSWORD_POLICY_VIOLATION:
    'Password must be 12–128 characters and include upper, lower, digit, and special characters.',
  INVALID_CREDENTIALS: 'Email or password is incorrect.',
  EMAIL_NOT_VERIFIED: 'Verify your email before signing in.',
  ACCOUNT_LOCKED: 'This account is locked. Contact support for help.',
  ACCOUNT_DISABLED: 'This account is disabled.',
  VERIFICATION_TOKEN_INVALID: 'Verification token is invalid.',
  VERIFICATION_TOKEN_EXPIRED: 'Verification token has expired.',
  VERIFICATION_TOKEN_CONSUMED: 'Verification token was already used.',
  VALIDATION_ERROR: 'Check your entries and try again.',
};

export function identityErrorMessage(error: unknown, fallback = 'Something went wrong'): string {
  if (isApiError(error) && error.code && IDENTITY_ERROR_MESSAGES[error.code]) {
    return IDENTITY_ERROR_MESSAGES[error.code];
  }

  if (isApiError(error)) {
    return error.message || fallback;
  }

  if (error instanceof Error) {
    return error.message;
  }

  return fallback;
}

export function getIdentityErrorMessage(code: string): string | undefined {
  return IDENTITY_ERROR_MESSAGES[code];
}

export { IDENTITY_ERROR_MESSAGES };
