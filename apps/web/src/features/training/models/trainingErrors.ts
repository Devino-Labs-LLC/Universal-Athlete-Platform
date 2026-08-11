import { isApiError } from '@/core/api/errors';

const TRAINING_ERROR_MESSAGES: Record<string, string> = {
  TRAINING_PLAN_NOT_FOUND: 'Training plan was not found.',
  WORKOUT_DAY_NOT_FOUND: 'Workout day was not found.',
  WORKOUT_EXERCISE_NOT_FOUND: 'Workout exercise was not found.',
  WORKOUT_OCCURRENCE_NOT_FOUND: 'Workout occurrence was not found.',
  TRAINING_PLAN_DELETE_NOT_ALLOWED: 'Only draft plans can be deleted.',
  TRAINING_PLAN_ARCHIVED: 'This plan is archived and cannot be modified.',
  INVALID_TRAINING_PLAN_STATUS: 'That plan status change is not allowed.',
  INVALID_TRAINING_PLAN_DATES: 'Plan dates are invalid.',
  INVALID_TRAINING_PLAN_SCHEDULE_STATUS: 'That schedule action is not allowed right now.',
  INVALID_TRAINING_PLAN_SCHEDULE_DATES: 'Schedule dates are invalid.',
  TRAINING_PLAN_SCHEDULE_NOT_CONFIGURED: 'Schedule must be activated before generating occurrences.',
  TRAINING_PLAN_SCHEDULE_REQUIRES_WORKOUT_DAYS: 'Add at least one workout day before activating the schedule.',
  INVALID_WORKOUT_OCCURRENCE_GENERATION_RANGE: 'Generation range must be 90 days or fewer.',
  WORKOUT_OCCURRENCE_GENERATION_CONFLICT: 'Occurrence generation conflict. Try again.',
  INVALID_TRAINING_CALENDAR_RANGE: 'Calendar range is too large.',
  WORKOUT_OCCURRENCE_RESCHEDULE_NOT_ALLOWED: 'This occurrence cannot be rescheduled.',
  WORKOUT_OCCURRENCE_DELETE_NOT_ALLOWED: 'This occurrence cannot be deleted.',
  DUPLICATE_WORKOUT_OCCURRENCE: 'An occurrence already exists for that date.',
  INVALID_WORKOUT_DAY_ORDER: 'Day order is invalid.',
  INVALID_WORKOUT_EXERCISE_ORDER: 'Exercise order is invalid.',
  DUPLICATE_WORKOUT_DAY: 'A day with that placement already exists.',
  DUPLICATE_WORKOUT_EXERCISE: 'That exercise is already on this day.',
  INVALID_TIMEZONE: 'Timezone is invalid.',
  TRAINING_ENVIRONMENT_NOT_FOUND: 'Training environment was not found.',
  INVALID_TRAINING_ENVIRONMENT_REFERENCE: 'Selected environment is not available.',
  EXERCISE_DEFINITION_NOT_FOUND: 'Exercise definition was not found.',
  VALIDATION_ERROR: 'Please check the form and try again.',
};

export function trainingErrorMessage(error: unknown): string {
  if (isApiError(error)) {
    const code = error.code;
    if (code && TRAINING_ERROR_MESSAGES[code]) {
      return TRAINING_ERROR_MESSAGES[code];
    }
    return error.message ?? 'Something went wrong.';
  }
  if (error instanceof Error) {
    return error.message;
  }
  return 'Something went wrong.';
}

export function isTrainingErrorCode(error: unknown, code: string): boolean {
  return isApiError(error) && error.code === code;
}

export { TRAINING_ERROR_MESSAGES };
