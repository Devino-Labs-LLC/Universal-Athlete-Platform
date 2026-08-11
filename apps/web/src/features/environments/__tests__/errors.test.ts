import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  environmentErrorMessage,
  isArchivedEnvironmentError,
  isDefaultConflictError,
  isDuplicateEnvironmentError,
} from '@/features/environments/models/errors';

function apiError(code: string) {
  return new ApiError('backend message', { category: 'VALIDATION', status: 422, code });
}

describe('environmentErrorMessage', () => {
  it('maps known error codes to friendly copy', () => {
    expect(environmentErrorMessage(apiError('TRAINING_ENVIRONMENT_NOT_FOUND'))).toBe(
      'Training environment was not found.',
    );
    expect(environmentErrorMessage(apiError('DUPLICATE_TRAINING_ENVIRONMENT'))).toBe(
      'An environment with this name already exists.',
    );
    expect(environmentErrorMessage(apiError('TRAINING_ENVIRONMENT_DEFAULT_CONFLICT'))).toBe(
      'Could not update the default environment. Try again or refresh the list.',
    );
  });

  it('falls back to the raw message for unmapped codes', () => {
    expect(environmentErrorMessage(apiError('SOME_UNMAPPED_CODE'))).toBe('backend message');
  });

  it('falls back to a generic message for non-ApiError values', () => {
    expect(environmentErrorMessage(new Error('boom'))).toBe('boom');
    expect(environmentErrorMessage('nope', 'fallback')).toBe('fallback');
  });

  it('detects duplicate name errors', () => {
    expect(isDuplicateEnvironmentError(apiError('DUPLICATE_TRAINING_ENVIRONMENT'))).toBe(true);
    expect(isDuplicateEnvironmentError(apiError('TRAINING_ENVIRONMENT_NOT_FOUND'))).toBe(false);
  });

  it('detects archived errors', () => {
    expect(isArchivedEnvironmentError(apiError('TRAINING_ENVIRONMENT_ARCHIVED'))).toBe(true);
  });

  it('detects default conflict errors', () => {
    expect(isDefaultConflictError(apiError('TRAINING_ENVIRONMENT_DEFAULT_CONFLICT'))).toBe(true);
  });
});
