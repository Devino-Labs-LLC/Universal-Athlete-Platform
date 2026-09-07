import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  coachErrorMessage,
  isCoachNotFoundError,
  isCoachUnauthorizedError,
} from '@/features/coach/models/errors';

describe('coach errors', () => {
  it('maps not-found and unauthorized categories', () => {
    expect(
      coachErrorMessage(new ApiError('x', { category: 'NOT_FOUND', status: 404 })),
    ).toBe('This team or athlete is unavailable.');
    expect(
      coachErrorMessage(new ApiError('x', { category: 'UNAUTHORIZED', status: 401 })),
    ).toBe('Your session expired. Sign in again to continue.');
    expect(isCoachNotFoundError(new ApiError('x', { category: 'NOT_FOUND' }))).toBe(true);
    expect(isCoachUnauthorizedError(new ApiError('x', { category: 'UNAUTHORIZED' }))).toBe(true);
  });
});
