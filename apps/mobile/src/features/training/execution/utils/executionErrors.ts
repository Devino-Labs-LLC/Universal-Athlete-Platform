import { isApiError } from '@/src/core/api/errors';

const EXECUTION_ERROR_MESSAGES: Record<string, string> = {
  INVALID_WORKOUT_OCCURRENCE_STATUS:
    'This workout cannot be updated in its current state. Refresh and try again.',
  WORKOUT_OCCURRENCE_HAS_INCOMPLETE_EXERCISES:
    'Complete or skip all exercises before finishing the workout.',
  INVALID_WORKOUT_EXERCISE_EXECUTION_STATUS:
    'This exercise cannot be updated in its current state.',
  WORKOUT_EXERCISE_EXECUTION_HAS_INCOMPLETE_SETS:
    'Complete or skip all sets before marking this exercise done.',
  WORKOUT_EXERCISE_EXECUTION_ACTUALS_ARE_SET_DERIVED:
    'Exercise totals come from logged sets. Update individual sets instead.',
  INVALID_WORKOUT_EXERCISE_SET_STATUS: 'This set cannot be updated in its current state.',
  WORKOUT_EXERCISE_SET_LIMIT_EXCEEDED: 'Maximum set count reached for this exercise.',
  WORKOUT_EXERCISE_SET_DELETE_NOT_ALLOWED:
    'This set cannot be deleted. At least one set must remain.',
  WORKOUT_SESSION_EFFORT_NOT_ALLOWED:
    'Session effort can only be logged after the workout is completed.',
  WORKOUT_SESSION_EFFORT_ALREADY_EXISTS:
    'Session effort already recorded. Edit the existing entry instead.',
  VALIDATION_ERROR: 'Check your entries and try again.',
  WORKOUT_LOAD_SUMMARY_NOT_FOUND: 'Training load summary is not available yet.',
};

export function executionErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    if (error.status === 409 && !error.code) {
      return 'Workout changed. Refreshing…';
    }
    if (error.code && EXECUTION_ERROR_MESSAGES[error.code]) {
      return EXECUTION_ERROR_MESSAGES[error.code];
    }
    return error.message;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong. Please try again.';
}

export function isConflictError(error: unknown): boolean {
  return isApiError(error) && error.status === 409;
}
