import { isApiError } from '@/core/api/errors';

const RECOVERY_ERROR_MESSAGES: Record<string, string> = {
  RECOVERY_CHECK_IN_ALREADY_EXISTS: 'A check-in already exists for this date. Open it to update instead.',
  RECOVERY_CHECK_IN_VERSION_CONFLICT:
    'This check-in was updated elsewhere. Review the latest version and try again.',
  RECOVERY_CHECK_IN_NOT_FOUND: 'Recovery check-in was not found.',
  RECOVERY_CHECK_IN_NOT_ACCESSIBLE: 'You do not have access to this check-in.',
  INVALID_RECOVERY_CHECK_IN_DATE: 'Check-in date is not valid.',
  RECOVERY_CHECK_IN_DATE_OUT_OF_RANGE: 'Check-in date is outside the allowed range.',
  INVALID_RECOVERY_CHECK_IN_DATE_RANGE: 'Date range is not valid.',
  INVALID_RECOVERY_CALENDAR_DATE_RANGE: 'Calendar date range cannot exceed 93 days.',
  EMPTY_RECOVERY_CHECK_IN: 'Add at least one recovery rating before saving.',
  INVALID_RECOVERY_CHECK_IN_NOTES: 'Notes exceed the maximum length.',
  INVALID_BODY_AREA_DISCOMFORT: 'Discomfort entry is not valid.',
  DUPLICATE_BODY_AREA_DISCOMFORT: 'Duplicate body area and side combination.',
  TOO_MANY_BODY_AREA_DISCOMFORT_OBSERVATIONS: 'Too many discomfort entries were provided.',
  INVALID_SLEEP_DURATION: 'Sleep duration is not valid.',
  INVALID_SLEEP_QUALITY: 'Sleep quality rating is not valid.',
  INVALID_FATIGUE_RATING: 'Fatigue rating is not valid.',
  INVALID_MUSCLE_SORENESS_RATING: 'Muscle soreness rating is not valid.',
  INVALID_STRESS_RATING: 'Stress rating is not valid.',
  INVALID_MOOD_RATING: 'Mood rating is not valid.',
  INVALID_TRAINING_MOTIVATION_RATING: 'Motivation rating is not valid.',
  INVALID_BODY_AREA: 'Body area is not recognized.',
  INVALID_BODY_SIDE: 'Body side is not recognized.',
  INVALID_DISCOMFORT_INTENSITY: 'Discomfort intensity is not valid.',
  INVALID_RECOVERY_BASELINE_WINDOW: 'Baseline window must be 7, 14, or 28 days.',
  INVALID_RECOVERY_TREND_DATE_RANGE: 'Trend date range is not valid.',
  INVALID_RECOVERY_METRIC_TYPE: 'That recovery metric type is not recognized.',
  RECOVERY_ANALYTICS_DATE_OUT_OF_RANGE: 'The requested date is outside the analytics range.',
  RECOVERY_ANALYTICS_CALCULATION_FAILED: 'Recovery analytics could not be calculated right now.',
  RECOVERY_OVERVIEW_LOAD_FAILED: 'The recovery overview could not be loaded right now.',
  DAILY_ATHLETE_STATE_SNAPSHOT_NOT_FOUND: 'That daily athlete state snapshot was not found.',
  DAILY_ATHLETE_STATE_SNAPSHOT_NOT_ACCESSIBLE: 'You do not have access to that athlete state snapshot.',
  INVALID_DAILY_ATHLETE_STATE_DATE: 'Date is not valid.',
  DAILY_ATHLETE_STATE_DATE_OUT_OF_RANGE: 'Date is outside the allowed range.',
  INVALID_DAILY_ATHLETE_STATE_BASELINE_WINDOW: 'Baseline window must be 7, 14, or 28 days.',
  DAILY_ATHLETE_STATE_VERSION_CONFLICT: 'The athlete state snapshot changed while generating. Try again.',
  DAILY_ATHLETE_STATE_GENERATION_FAILED: 'Daily athlete state could not be generated right now.',
  DAILY_ATHLETE_STATE_SOURCE_INCONSISTENT: 'Source data changed; regenerate the snapshot.',
  DAILY_ATHLETE_STATE_SNAPSHOT_COMPARE_INVALID: 'Those two snapshots cannot be compared.',
  DAILY_READINESS_ASSESSMENT_NOT_FOUND: 'That readiness assessment was not found.',
  DAILY_READINESS_ASSESSMENT_NOT_ACCESSIBLE: 'You do not have access to that readiness assessment.',
  DAILY_READINESS_STATE_SNAPSHOT_REQUIRED: 'A daily athlete state snapshot is required first.',
  DAILY_READINESS_CALCULATION_FAILED: 'Readiness could not be calculated right now.',
  DAILY_READINESS_COMPARE_INVALID: 'Those two assessments cannot be compared.',
  INVALID_DAILY_READINESS_DATE_RANGE: 'Date range is not valid.',
  INVALID_READINESS_ALGORITHM_VERSION: 'That readiness algorithm version is not recognized.',
  DAILY_TRAINING_RECOMMENDATION_NOT_FOUND: 'That training recommendation was not found.',
  DAILY_TRAINING_RECOMMENDATION_NOT_ACCESSIBLE: 'You do not have access to that recommendation.',
  DAILY_TRAINING_RECOMMENDATION_READINESS_REQUIRED: 'A readiness assessment is required first.',
  DAILY_TRAINING_RECOMMENDATION_CALCULATION_FAILED: 'The recommendation could not be calculated right now.',
  DAILY_TRAINING_RECOMMENDATION_COMPARE_INVALID: 'Those two recommendations cannot be compared.',
  INVALID_TRAINING_RECOMMENDATION_DATE_RANGE: 'Date range is not valid.',
  INVALID_TRAINING_RECOMMENDATION_ALGORITHM_VERSION: 'That recommendation algorithm version is not recognized.',
  VALIDATION_ERROR: 'Check your entries and try again.',
};

export function recoveryErrorMessage(error: unknown, fallback = 'Something went wrong.'): string {
  if (isApiError(error)) {
    if (error.code && RECOVERY_ERROR_MESSAGES[error.code]) {
      return RECOVERY_ERROR_MESSAGES[error.code];
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isRecoveryErrorCode(error: unknown, code: string): boolean {
  return isApiError(error) && error.code === code;
}

export function isVersionConflictError(error: unknown): boolean {
  return (
    isApiError(error) &&
    (error.code === 'RECOVERY_CHECK_IN_VERSION_CONFLICT' || error.code === 'VERSION_CONFLICT')
  );
}

export function isCheckInAlreadyExistsError(error: unknown): boolean {
  return isRecoveryErrorCode(error, 'RECOVERY_CHECK_IN_ALREADY_EXISTS');
}

export function isNotFoundError(error: unknown): boolean {
  return isApiError(error) && (error.status === 404 || error.category === 'NOT_FOUND');
}

export { RECOVERY_ERROR_MESSAGES };
