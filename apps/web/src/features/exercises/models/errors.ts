import { isApiError } from '@/core/api/errors';

const EXERCISE_ERROR_MESSAGES: Record<string, string> = {
  EXERCISE_DEFINITION_NOT_FOUND: 'Exercise was not found.',
  EXERCISE_DEFINITION_NOT_ACCESSIBLE: 'You do not have access to this exercise.',
  EXERCISE_DEFINITION_ARCHIVED: 'This exercise is archived and cannot be modified.',
  DUPLICATE_EXERCISE_DEFINITION: 'An exercise with this name already exists.',
  INVALID_EXERCISE_DEFINITION_NAME: 'Exercise name is not valid.',
  INVALID_EXERCISE_DEFINITION_METADATA: 'Exercise metadata is not valid.',
  EXERCISE_METADATA_PRIMARY_SECONDARY_CONFLICT:
    'A movement pattern or muscle group cannot be listed as both primary and secondary.',
  EXERCISE_EQUIPMENT_REQUIRED_OPTIONAL_CONFLICT:
    'Equipment cannot be listed as both required and optional.',
  INVALID_EXERCISE_DEFINITION_QUERY: 'That search is not valid.',
  SYSTEM_EXERCISE_DEFINITION_MODIFICATION_NOT_ALLOWED: 'System exercises cannot be modified.',
  DUPLICATE_EXERCISE_SUBSTITUTION_RELATIONSHIP: 'That substitution relationship already exists.',
  CONFLICTING_EQUIPMENT_CONTEXT_FILTERS:
    'Choose either an equipment filter or an environment filter, not both.',
  TRAINING_ENVIRONMENT_NOT_FOUND: 'Training environment was not found.',
  TRAINING_ENVIRONMENT_NOT_ACCESSIBLE: 'You do not have access to this training environment.',
  VALIDATION_ERROR: 'Please check the form and try again.',
};

export function exerciseErrorMessage(error: unknown, fallback = 'Something went wrong.'): string {
  if (isApiError(error)) {
    const code = error.code;
    if (code && EXERCISE_ERROR_MESSAGES[code]) {
      return EXERCISE_ERROR_MESSAGES[code];
    }
    if (code?.startsWith('EXERCISE_SUBSTITUTION_RELATIONSHIP_')) {
      return 'That substitution relationship request could not be completed.';
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isExerciseErrorCode(error: unknown, code: string): boolean {
  return isApiError(error) && error.code === code;
}

export function isSystemModificationError(error: unknown): boolean {
  return isExerciseErrorCode(error, 'SYSTEM_EXERCISE_DEFINITION_MODIFICATION_NOT_ALLOWED');
}

export function isConflictingEquipmentContextError(error: unknown): boolean {
  return isExerciseErrorCode(error, 'CONFLICTING_EQUIPMENT_CONTEXT_FILTERS');
}

export { EXERCISE_ERROR_MESSAGES };
