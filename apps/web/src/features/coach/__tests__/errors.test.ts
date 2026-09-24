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
    expect(
      coachErrorMessage(
        new ApiError('not applied', {
          category: 'CONFLICT',
          status: 409,
          code: 'BILLING_PAYMENT_NOT_APPLIED',
        }),
      ),
    ).toBe(
      'The plan change was not applied because payment could not be collected. Your current plan is unchanged.',
    );
    expect(
      coachErrorMessage(
        new ApiError('down', {
          category: 'SERVER',
          status: 502,
          code: 'BILLING_PROVIDER_UNAVAILABLE',
        }),
      ),
    ).toBe('Billing is temporarily unavailable. Try again.');
    expect(
      coachErrorMessage(
        new ApiError('capacity', {
          category: 'CONFLICT',
          status: 409,
          code: 'ORGANIZATION_PLAN_CAPACITY_CONFLICT',
        }),
      ),
    ).not.toContain('access to this capability');
  });
});
