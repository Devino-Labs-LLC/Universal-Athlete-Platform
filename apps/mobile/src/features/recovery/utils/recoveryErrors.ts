import { isApiError } from '@/src/core/api/errors';

const RECOVERY_ERROR_MESSAGES: Record<string, string> = {
  RECOVERY_CHECK_IN_ALREADY_EXISTS:
    'A check-in already exists for this date. Open it to update instead.',
  RECOVERY_CHECK_IN_VERSION_CONFLICT:
    'This check-in was updated elsewhere. Review the latest version and try again.',
  RECOVERY_CHECK_IN_NOT_FOUND: 'Recovery check-in not found.',
  RECOVERY_CHECK_IN_NOT_ACCESSIBLE: 'You do not have access to this check-in.',
  INVALID_RECOVERY_CHECK_IN_DATE: 'Check-in date is not valid.',
  RECOVERY_CHECK_IN_DATE_OUT_OF_RANGE: 'Check-in date is outside the allowed range.',
  INVALID_RECOVERY_CHECK_IN_DATE_RANGE: 'Date range is not valid.',
  EMPTY_RECOVERY_CHECK_IN: 'Add at least one recovery rating before saving.',
  INVALID_RECOVERY_CHECK_IN_NOTES: 'Notes exceed the maximum length.',
  INVALID_BODY_AREA_DISCOMFORT: 'Discomfort entry is not valid.',
  DUPLICATE_BODY_AREA_DISCOMFORT: 'Duplicate body area and side combination.',
  VALIDATION_ERROR: 'Check your entries and try again.',
};

export function recoveryErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code && RECOVERY_ERROR_MESSAGES[error.code]) {
      return RECOVERY_ERROR_MESSAGES[error.code];
    }
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong. Please try again.';
}

export function isVersionConflictError(error: unknown): boolean {
  return (
    isApiError(error) &&
    error.status === 409 &&
    (error.code === 'RECOVERY_CHECK_IN_VERSION_CONFLICT' || error.code === 'VERSION_CONFLICT')
  );
}

export function isCheckInAlreadyExistsError(error: unknown): boolean {
  return isApiError(error) && error.code === 'RECOVERY_CHECK_IN_ALREADY_EXISTS';
}

export function isNotFoundError(error: unknown): boolean {
  return isApiError(error) && (error.status === 404 || error.category === 'notFound');
}
