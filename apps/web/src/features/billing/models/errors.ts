import { isApiError } from '@/core/api/errors';

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
};

export function accountBillingErrorMessage(
  error: unknown,
  fallback = 'Unable to update billing.',
): string {
  if (isApiError(error)) {
    if (error.code && billingMessages[error.code]) {
      return billingMessages[error.code];
    }
    if (error.category === 'UNAUTHORIZED') {
      return 'Your session expired. Sign in again to continue.';
    }
    if (error.category === 'COMMERCIAL_ENTITLEMENT' || error.code === 'COMMERCIAL_ENTITLEMENT_REQUIRED') {
      return 'Premium access is not available for this account right now.';
    }
    if (error.category === 'CONFLICT') {
      return error.message || fallback;
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

/** Enabled Stripe with no Account subscription. */
export function isAccountBillingMissing(error: unknown): boolean {
  return isApiError(error) && error.category === 'NOT_FOUND' && error.code === 'ACCOUNT_NOT_FOUND';
}

/**
 * Stripe billing controllers are absent when disabled — Spring returns a generic 404.
 * Treat non-ACCOUNT_NOT_FOUND not-found as “billing unavailable” so the page degrades.
 */
export function isAccountBillingUnavailable(error: unknown): boolean {
  return isApiError(error) && error.category === 'NOT_FOUND' && error.code !== 'ACCOUNT_NOT_FOUND';
}
