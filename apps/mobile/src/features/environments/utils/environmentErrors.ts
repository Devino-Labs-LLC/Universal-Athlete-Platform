import { isApiError } from '@/src/core/api/errors';

const ENVIRONMENT_ERROR_MESSAGES: Record<string, string> = {
  TRAINING_ENVIRONMENT_NOT_FOUND: 'Training environment not found.',
  TRAINING_ENVIRONMENT_NOT_ACCESSIBLE: 'You do not have access to this training environment.',
  TRAINING_ENVIRONMENT_ARCHIVED:
    'This environment is archived and cannot be updated.',
  DUPLICATE_TRAINING_ENVIRONMENT:
    'An environment with this name already exists. Choose a different name.',
  INVALID_TRAINING_ENVIRONMENT_NAME: 'Environment name is not valid.',
  INVALID_TRAINING_ENVIRONMENT_EQUIPMENT: 'One or more equipment selections are not valid.',
  TRAINING_ENVIRONMENT_DEFAULT_CONFLICT:
    'Could not update the default environment. Try again or refresh the list.',
  INVALID_TRAINING_ENVIRONMENT_REFERENCE: 'The selected training environment is not valid.',
  WORKOUT_OCCURRENCE_ENVIRONMENT_LOCKED:
    'The training environment is locked for this workout and cannot be changed.',
  WORKOUT_OCCURRENCE_ENVIRONMENT_NOT_SET:
    'No actual environment is set for this workout.',
};

export function environmentErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.code && ENVIRONMENT_ERROR_MESSAGES[error.code]) {
      return ENVIRONMENT_ERROR_MESSAGES[error.code];
    }
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong. Please try again.';
}

export function isEnvironmentLockedError(error: unknown): boolean {
  return isApiError(error) && error.code === 'WORKOUT_OCCURRENCE_ENVIRONMENT_LOCKED';
}

export function isDuplicateEnvironmentError(error: unknown): boolean {
  return isApiError(error) && error.code === 'DUPLICATE_TRAINING_ENVIRONMENT';
}

export function isArchivedEnvironmentError(error: unknown): boolean {
  return isApiError(error) && error.code === 'TRAINING_ENVIRONMENT_ARCHIVED';
}
