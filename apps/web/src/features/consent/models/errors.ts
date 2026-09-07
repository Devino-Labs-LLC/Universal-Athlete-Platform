import { isApiError } from '@/core/api/errors';

const CONSENT_ERROR_MESSAGES: Record<string, string> = {
  CONSENT_NOT_FOUND:
    'This sharing grant was not found. It may have been revoked, or you may not have access.',
  ACTIVE_GRANT_EXISTS:
    'You already have an active sharing grant for this team membership. Revoke it before creating a new one.',
  TEAM_ARCHIVED: 'This team is archived and cannot receive new sharing grants.',
  ORGANIZATION_ARCHIVED: 'This organization is archived and cannot receive new sharing grants.',
  VALIDATION_ERROR: 'Please check your selections and try again.',
  OPTIMISTIC_LOCK_CONFLICT: 'Something changed concurrently. Please retry.',
};

export function consentErrorMessage(error: unknown, fallback = 'Something went wrong.'): string {
  if (isApiError(error)) {
    if (error.code && CONSENT_ERROR_MESSAGES[error.code]) {
      return CONSENT_ERROR_MESSAGES[error.code];
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export { CONSENT_ERROR_MESSAGES };
