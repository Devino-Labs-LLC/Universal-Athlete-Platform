import { isTrainingErrorCode, trainingErrorMessage, TRAINING_ERROR_MESSAGES } from '@/features/training/models/trainingErrors';
import { ApiError } from '@/core/api/errors';

describe('trainingErrors', () => {
  it('maps known training error codes', () => {
    expect(TRAINING_ERROR_MESSAGES.TRAINING_PLAN_NOT_FOUND).toContain('not found');
    expect(TRAINING_ERROR_MESSAGES.INVALID_WORKOUT_OCCURRENCE_GENERATION_RANGE).toBeDefined();
  });

  it('returns friendly message for ApiError', () => {
    expect(
      trainingErrorMessage(
        new ApiError('Training plan was not found', {
          category: 'NOT_FOUND',
          code: 'TRAINING_PLAN_NOT_FOUND',
          status: 404,
        }),
      ),
    ).toContain('not found');
  });

  it('detects specific error code', () => {
    expect(
      isTrainingErrorCode(
        new ApiError('Delete not allowed', {
          category: 'CONFLICT',
          code: 'WORKOUT_OCCURRENCE_DELETE_NOT_ALLOWED',
          status: 409,
        }),
        'WORKOUT_OCCURRENCE_DELETE_NOT_ALLOWED',
      ),
    ).toBe(true);
  });

  it('falls back for unknown errors', () => {
    expect(trainingErrorMessage(new Error('boom'))).toBe('boom');
  });
});
