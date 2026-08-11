import { isApiError } from '@/core/api/errors';

const PERFORMANCE_ERROR_MESSAGES: Record<string, string> = {
  EXERCISE_PERFORMANCE_KEY_NOT_FOUND: 'No training history exists for this exercise yet.',
  EXERCISE_DEFINITION_NOT_FOUND: 'That exercise could not be found.',
  INVALID_TRAINING_PERFORMANCE_RANGE: 'The requested date range is not valid.',
  PERSONAL_RECORD_REBUILD_CONFLICT: 'Personal records changed while loading. Refresh and try again.',
  WORKOUT_LOAD_SUMMARY_NOT_FOUND: 'A training load summary was not found for this session.',
  WORKOUT_LOAD_CALCULATION_FAILED: 'Training load could not be calculated right now.',
  TRAINING_LOAD_NUMERIC_OVERFLOW: 'That calculation produced a value that is too large to display.',
  INVALID_TRAINING_LOAD_DATE_RANGE: 'The training load date range is not valid.',
  INVALID_TRAINING_LOAD_GRANULARITY: 'That training load granularity is not recognized.',
  TRAINING_LOAD_REBUILD_CONFLICT: 'Training load changed while rebuilding. Try again.',
  TRAINING_LOAD_REBUILD_FAILED: 'Training load could not be rebuilt right now.',
  TRAINING_METRICS_REQUIRE_COMPLETED_EXECUTION: 'Metrics are only available after the exercise is completed.',
  TRAINING_METRICS_REQUIRE_COMPLETED_SETS: 'Metrics require at least one completed set.',
  VALIDATION_ERROR: 'Check your entries and try again.',
};

export function performanceErrorMessage(error: unknown, fallback = 'Something went wrong loading performance data.'): string {
  if (isApiError(error)) {
    if (error.code && PERFORMANCE_ERROR_MESSAGES[error.code]) {
      return PERFORMANCE_ERROR_MESSAGES[error.code];
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isPerformanceErrorCode(error: unknown, code: string): boolean {
  return isApiError(error) && error.code === code;
}

export function isPerformanceNotFoundError(error: unknown): boolean {
  return isApiError(error) && (error.status === 404 || error.category === 'NOT_FOUND');
}

export function isExercisePerformanceKeyNotFound(error: unknown): boolean {
  return isPerformanceErrorCode(error, 'EXERCISE_PERFORMANCE_KEY_NOT_FOUND');
}

export { PERFORMANCE_ERROR_MESSAGES };
