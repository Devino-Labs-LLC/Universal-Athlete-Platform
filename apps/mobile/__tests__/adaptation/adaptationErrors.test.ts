import { ApiError } from '@/src/core/api/errors';
import {
  adaptationErrorMessage,
  isActiveProposalExistsError,
  isSubstitutionLockedError,
  isVersionConflictError,
} from '@/src/features/adaptation/utils/adaptationErrors';

describe('adaptationErrors', () => {
  it('maps known backend codes to friendly messages', () => {
    const error = new ApiError('conflict', {
      category: 'conflict',
      status: 409,
      code: 'WORKOUT_ADAPTATION_PROPOSAL_VERSION_CONFLICT',
    });
    expect(adaptationErrorMessage(error)).toContain('changed on the server');
    expect(isVersionConflictError(error)).toBe(true);
  });

  it('detects active proposal and substitution locked errors', () => {
    const active = new ApiError('exists', {
      category: 'conflict',
      status: 409,
      code: 'ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS',
    });
    const locked = new ApiError('locked', {
      category: 'conflict',
      status: 409,
      code: 'WORKOUT_EXERCISE_SUBSTITUTION_LOCKED',
    });
    expect(isActiveProposalExistsError(active)).toBe(true);
    expect(isSubstitutionLockedError(locked)).toBe(true);
  });

  it('falls back to generic message for unknown errors', () => {
    expect(adaptationErrorMessage(new Error('boom'))).toBe('boom');
  });
});
