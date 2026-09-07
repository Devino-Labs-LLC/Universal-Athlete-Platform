import { isApiError } from '@/src/core/api/errors';

const CONSENT_ERROR_MESSAGES: Record<string, string> = {
  CONSENT_NOT_FOUND: 'This sharing grant was not found or is no longer available.',
  ACTIVE_GRANT_EXISTS:
    'You already have an active sharing grant for this team membership. Revoke it before granting again.',
  TEAM_ARCHIVED: 'This team is archived and cannot receive new sharing grants.',
  ORGANIZATION_ARCHIVED: 'This organization is archived and cannot receive new sharing grants.',
  OPTIMISTIC_LOCK_CONFLICT: 'Something changed concurrently. Please try again.',
  VALIDATION_ERROR: 'Check your team and selected scopes, then try again.',
};

export function consentErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code && CONSENT_ERROR_MESSAGES[error.code]) {
      return CONSENT_ERROR_MESSAGES[error.code];
    }
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong. Please try again.';
}

export function consentErrorTitle(error: unknown): string {
  if (isApiError(error) && error.code === 'ACTIVE_GRANT_EXISTS') {
    return 'Already sharing';
  }
  if (isApiError(error) && error.code === 'CONSENT_NOT_FOUND') {
    return 'Sharing unavailable';
  }
  if (
    isApiError(error) &&
    (error.code === 'TEAM_ARCHIVED' || error.code === 'ORGANIZATION_ARCHIVED')
  ) {
    return 'Team unavailable';
  }
  return 'Something went wrong';
}
