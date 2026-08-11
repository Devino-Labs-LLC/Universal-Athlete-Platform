import { isApiError } from '@/core/api/errors';

const ENVIRONMENT_ERROR_MESSAGES: Record<string, string> = {
  TRAINING_ENVIRONMENT_NOT_FOUND: 'Training environment was not found.',
  TRAINING_ENVIRONMENT_NOT_ACCESSIBLE: 'You do not have access to this training environment.',
  DUPLICATE_TRAINING_ENVIRONMENT: 'An environment with this name already exists.',
  TRAINING_ENVIRONMENT_ARCHIVED: 'This environment is archived and cannot be updated.',
  INVALID_TRAINING_ENVIRONMENT_NAME: 'Environment name is not valid.',
  INVALID_TRAINING_ENVIRONMENT_EQUIPMENT: 'One or more equipment selections are not valid.',
  TRAINING_ENVIRONMENT_DEFAULT_CONFLICT:
    'Could not update the default environment. Try again or refresh the list.',
  VALIDATION_ERROR: 'Please check the form and try again.',
};

export function environmentErrorMessage(error: unknown, fallback = 'Something went wrong.'): string {
  if (isApiError(error)) {
    if (error.code && ENVIRONMENT_ERROR_MESSAGES[error.code]) {
      return ENVIRONMENT_ERROR_MESSAGES[error.code];
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isDuplicateEnvironmentError(error: unknown): boolean {
  return isApiError(error) && error.code === 'DUPLICATE_TRAINING_ENVIRONMENT';
}

export function isArchivedEnvironmentError(error: unknown): boolean {
  return isApiError(error) && error.code === 'TRAINING_ENVIRONMENT_ARCHIVED';
}

export function isDefaultConflictError(error: unknown): boolean {
  return isApiError(error) && error.code === 'TRAINING_ENVIRONMENT_DEFAULT_CONFLICT';
}

export { ENVIRONMENT_ERROR_MESSAGES };
