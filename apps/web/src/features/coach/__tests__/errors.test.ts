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
    expect(
      coachErrorMessage(
        new ApiError('x', {
          category: 'COMMERCIAL_ENTITLEMENT',
          status: 402,
          code: 'COMMERCIAL_ENTITLEMENT_REQUIRED',
        }),
      ),
    ).toBe('This organization does not currently have access to this capability.');
    expect(
      coachErrorMessage(
        new ApiError('x', {
          category: 'COMMERCIAL_ENTITLEMENT',
          status: 402,
          code: 'COMMERCIAL_ENTITLEMENT_REQUIRED',
        }),
      ),
    ).not.toContain('session expired');
    expect(isCoachNotFoundError(new ApiError('x', { category: 'NOT_FOUND' }))).toBe(true);
    expect(isCoachUnauthorizedError(new ApiError('x', { category: 'UNAUTHORIZED' }))).toBe(true);
  });
});
