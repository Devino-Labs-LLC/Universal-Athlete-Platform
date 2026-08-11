import { ApiError } from '@/src/core/api/errors';
import {
  environmentErrorMessage,
  isArchivedEnvironmentError,
  isDuplicateEnvironmentError,
  isEnvironmentLockedError,
} from '@/src/features/environments/utils/environmentErrors';

describe('environmentErrors', () => {
  it('maps known environment error codes', () => {
    expect(
      environmentErrorMessage(
        new ApiError('duplicate', { code: 'DUPLICATE_TRAINING_ENVIRONMENT', category: 'conflict' }),
      ),
    ).toContain('already exists');
    expect(
      environmentErrorMessage(
        new ApiError('locked', {
          code: 'WORKOUT_OCCURRENCE_ENVIRONMENT_LOCKED',
          category: 'conflict',
        }),
      ),
    ).toContain('locked');
    expect(
      environmentErrorMessage(
        new ApiError('archived', { code: 'TRAINING_ENVIRONMENT_ARCHIVED', category: 'conflict' }),
      ),
    ).toContain('archived');
  });

  it('detects helper error types', () => {
    const locked = new ApiError('locked', {
      code: 'WORKOUT_OCCURRENCE_ENVIRONMENT_LOCKED',
      category: 'conflict',
    });
    expect(isEnvironmentLockedError(locked)).toBe(true);
    expect(
      isDuplicateEnvironmentError(
        new ApiError('dup', { code: 'DUPLICATE_TRAINING_ENVIRONMENT', category: 'conflict' }),
      ),
    ).toBe(true);
    expect(
      isArchivedEnvironmentError(
        new ApiError('arch', { code: 'TRAINING_ENVIRONMENT_ARCHIVED', category: 'conflict' }),
      ),
    ).toBe(true);
  });
});
