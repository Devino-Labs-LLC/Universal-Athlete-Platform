import { executionErrorMessage } from '@/src/features/training/execution/utils/executionErrors';
import { ApiError } from '@/src/core/api/errors';

describe('executionErrors', () => {
  it('maps known stable codes', () => {
    const error = new ApiError('Conflict', {
      category: 'conflict',
      status: 409,
      code: 'WORKOUT_EXERCISE_EXECUTION_HAS_INCOMPLETE_SETS',
    });
    expect(executionErrorMessage(error)).toContain('Complete or skip all sets');
  });

  it('maps session effort already exists', () => {
    const error = new ApiError('Conflict', {
      category: 'conflict',
      status: 409,
      code: 'WORKOUT_SESSION_EFFORT_ALREADY_EXISTS',
    });
    expect(executionErrorMessage(error)).toContain('already recorded');
  });

  it('maps generic 409 without code', () => {
    const error = new ApiError('Conflict', {
      category: 'conflict',
      status: 409,
    });
    expect(executionErrorMessage(error)).toContain('Refreshing');
  });

  it('falls back to error message', () => {
    expect(executionErrorMessage(new Error('Network down'))).toBe('Network down');
  });
});
