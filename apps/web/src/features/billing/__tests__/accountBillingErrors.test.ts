import { describe, expect, it } from 'vitest';

import { ApiError } from '@/core/api/errors';
import {
  accountBillingErrorMessage,
  isAccountBillingMissing,
  isAccountBillingUnavailable,
} from '@/features/billing/models/errors';

describe('account billing errors', () => {
  it('maps known billing codes to athlete-facing copy', () => {
    expect(
      accountBillingErrorMessage(
        new ApiError('raw', { category: 'CONFLICT', code: 'BILLING_CHECKOUT_IN_PROGRESS' }),
      ),
    ).toBe('Checkout is already in progress.');
    expect(
      accountBillingErrorMessage(
        new ApiError('raw', { category: 'CONFLICT', code: 'BILLING_SUBSCRIPTION_EXISTS' }),
      ),
    ).toBe('An Individual Premium subscription already exists for this account.');
    expect(
      accountBillingErrorMessage(
        new ApiError('raw', { category: 'SERVER', code: 'BILLING_PROVIDER_UNAVAILABLE' }),
      ),
    ).toBe('Billing is temporarily unavailable. Try again.');
    expect(
      accountBillingErrorMessage(
        new ApiError('raw', { category: 'CONFLICT', code: 'BILLING_REQUEST_CONFLICT' }),
      ),
    ).toBe('That billing request was already used for a different change.');
    expect(
      accountBillingErrorMessage(
        new ApiError('raw', { category: 'CONFLICT', code: 'BILLING_CADENCE_MISSING' }),
      ),
    ).toBe('Subscription billing cadence is unavailable.');
    expect(
      accountBillingErrorMessage(
        new ApiError('raw', { category: 'CONFLICT', code: 'BILLING_CONCURRENT_MODIFICATION' }),
      ),
    ).toBe('Billing state changed. Refresh and try again.');
    expect(
      accountBillingErrorMessage(
        new ApiError('raw', { category: 'CONFLICT', code: 'BILLING_SUBSCRIPTION_STATE_CONFLICT' }),
      ),
    ).toBe('Account billing needs attention before it can be managed.');
    expect(
      accountBillingErrorMessage(
        new ApiError('raw', { category: 'CONFLICT', code: 'BILLING_MANAGED_BY_ORIGIN_PROVIDER' }),
      ),
    ).toBe(
      'Manage this Premium subscription through the store or channel where it was purchased.',
    );
  });

  it('maps auth entitlement conflict and generic api fallbacks', () => {
    expect(
      accountBillingErrorMessage(
        new ApiError('session gone', { category: 'UNAUTHORIZED', status: 401 }),
      ),
    ).toBe('Your session expired. Sign in again to continue.');
    expect(
      accountBillingErrorMessage(
        new ApiError('denied', {
          category: 'COMMERCIAL_ENTITLEMENT',
          code: 'COMMERCIAL_ENTITLEMENT_REQUIRED',
        }),
      ),
    ).toBe('Premium access is not available for this account right now.');
    expect(
      accountBillingErrorMessage(
        new ApiError('conflict detail', { category: 'CONFLICT', status: 409 }),
      ),
    ).toBe('conflict detail');
    expect(
      accountBillingErrorMessage(
        new ApiError('', { category: 'CONFLICT', status: 409 }),
        'Unable to update billing.',
      ),
    ).toBe('Unable to update billing.');
    expect(
      accountBillingErrorMessage(new ApiError('server blew up', { category: 'SERVER', status: 500 })),
    ).toBe('server blew up');
  });

  it('maps plain errors and unknown values to fallbacks', () => {
    expect(accountBillingErrorMessage(new Error('plain failure'))).toBe('plain failure');
    expect(accountBillingErrorMessage('not-an-error', 'Unable to update billing.')).toBe(
      'Unable to update billing.',
    );
  });

  it('detects missing vs unavailable account billing not-found responses', () => {
    expect(
      isAccountBillingMissing(
        new ApiError('missing', { category: 'NOT_FOUND', code: 'ACCOUNT_NOT_FOUND' }),
      ),
    ).toBe(true);
    expect(
      isAccountBillingMissing(new ApiError('missing', { category: 'NOT_FOUND', status: 404 })),
    ).toBe(false);
    expect(
      isAccountBillingUnavailable(new ApiError('gone', { category: 'NOT_FOUND', status: 404 })),
    ).toBe(true);
    expect(
      isAccountBillingUnavailable(
        new ApiError('missing', { category: 'NOT_FOUND', code: 'ACCOUNT_NOT_FOUND' }),
      ),
    ).toBe(false);
  });
});
