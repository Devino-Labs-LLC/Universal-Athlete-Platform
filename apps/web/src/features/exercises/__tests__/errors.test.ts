import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  exerciseErrorMessage,
  isConflictingEquipmentContextError,
  isSystemModificationError,
} from '@/features/exercises/models/errors';

function apiError(code: string) {
  return new ApiError('backend message', { category: 'VALIDATION', status: 422, code });
}

describe('exerciseErrorMessage', () => {
  it('maps known error codes to friendly copy', () => {
    expect(exerciseErrorMessage(apiError('EXERCISE_DEFINITION_NOT_FOUND'))).toBe('Exercise was not found.');
    expect(exerciseErrorMessage(apiError('DUPLICATE_EXERCISE_DEFINITION'))).toBe(
      'An exercise with this name already exists.',
    );
    expect(exerciseErrorMessage(apiError('SYSTEM_EXERCISE_DEFINITION_MODIFICATION_NOT_ALLOWED'))).toBe(
      'System exercises cannot be modified.',
    );
    expect(exerciseErrorMessage(apiError('CONFLICTING_EQUIPMENT_CONTEXT_FILTERS'))).toBe(
      'Choose either an equipment filter or an environment filter, not both.',
    );
  });

  it('maps EXERCISE_SUBSTITUTION_RELATIONSHIP_* wildcard codes', () => {
    expect(exerciseErrorMessage(apiError('EXERCISE_SUBSTITUTION_RELATIONSHIP_NOT_FOUND'))).toBe(
      'That substitution relationship request could not be completed.',
    );
  });

  it('falls back to the raw API message for unknown codes', () => {
    expect(exerciseErrorMessage(apiError('SOME_UNMAPPED_CODE'))).toBe('backend message');
  });

  it('falls back to a generic message for non-ApiError values', () => {
    expect(exerciseErrorMessage(new Error('boom'))).toBe('boom');
    expect(exerciseErrorMessage('not an error', 'fallback copy')).toBe('fallback copy');
  });

  it('detects system modification errors', () => {
    expect(isSystemModificationError(apiError('SYSTEM_EXERCISE_DEFINITION_MODIFICATION_NOT_ALLOWED'))).toBe(
      true,
    );
    expect(isSystemModificationError(apiError('EXERCISE_DEFINITION_NOT_FOUND'))).toBe(false);
  });

  it('detects conflicting equipment/environment context filter errors', () => {
    expect(isConflictingEquipmentContextError(apiError('CONFLICTING_EQUIPMENT_CONTEXT_FILTERS'))).toBe(true);
    expect(isConflictingEquipmentContextError(apiError('EXERCISE_DEFINITION_NOT_FOUND'))).toBe(false);
  });
});
