import { isApiError } from '@/src/core/api/errors';

const billingMessages: Record<string, string> = {
  BILLING_LIFECYCLE_CONFLICT: 'This subscription cannot be changed in its current state.',
  BILLING_SUBSCRIPTION_NOT_MANAGEABLE: 'This subscription cannot be managed.',
  BILLING_SUBSCRIPTION_STATE_CONFLICT:
    'Account billing needs attention before it can be managed.',
  BILLING_REQUEST_CONFLICT: 'That billing request was already used for a different change.',
  BILLING_SUBSCRIPTION_EXISTS: 'An Individual Premium subscription already exists for this account.',
  BILLING_CHECKOUT_IN_PROGRESS: 'Checkout is already in progress.',
  BILLING_PROVIDER_UNAVAILABLE: 'Billing is temporarily unavailable. Try again.',
  BILLING_CADENCE_MISSING: 'Subscription billing cadence is unavailable.',
  BILLING_CONCURRENT_MODIFICATION: 'Billing state changed. Refresh and try again.',
  BILLING_MANAGED_BY_ORIGIN_PROVIDER:
    'Manage this Premium subscription through the store or channel where it was purchased.',
};

export function accountBillingErrorMessage(
  error: unknown,
  fallback = 'Unable to update billing.',
): string {
  if (isApiError(error)) {
    if (error.code && billingMessages[error.code]) {
      return billingMessages[error.code];
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

/** Enabled billing with no Account subscription. */
export function isAccountBillingMissing(error: unknown): boolean {
  return isApiError(error) && error.category === 'notFound' && error.code === 'ACCOUNT_NOT_FOUND';
}

/**
 * Billing controllers absent when disabled — generic not-found.
 * Treat non-ACCOUNT_NOT_FOUND not-found as billing unavailable.
 */
export function isAccountBillingUnavailable(error: unknown): boolean {
  return isApiError(error) && error.category === 'notFound' && error.code !== 'ACCOUNT_NOT_FOUND';
}
