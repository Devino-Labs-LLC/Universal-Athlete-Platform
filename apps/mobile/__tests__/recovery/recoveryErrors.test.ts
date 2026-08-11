import {
  isCheckInAlreadyExistsError,
  isNotFoundError,
  isVersionConflictError,
  recoveryErrorMessage,
} from '@/src/features/recovery/utils/recoveryErrors';
import { ApiError } from '@/src/core/api/errors';

describe('recoveryErrors', () => {
  it('maps known recovery error codes', () => {
    const error = new ApiError('conflict', {
      category: 'conflict',
      status: 409,
      code: 'RECOVERY_CHECK_IN_VERSION_CONFLICT',
    });
    expect(recoveryErrorMessage(error)).toContain('updated elsewhere');
    expect(isVersionConflictError(error)).toBe(true);
  });

  it('detects check-in already exists', () => {
    const error = new ApiError('exists', {
      category: 'conflict',
      status: 409,
      code: 'RECOVERY_CHECK_IN_ALREADY_EXISTS',
    });
    expect(isCheckInAlreadyExistsError(error)).toBe(true);
  });

  it('detects not found errors', () => {
    const error = new ApiError('missing', { category: 'notFound', status: 404 });
    expect(isNotFoundError(error)).toBe(true);
  });
});
