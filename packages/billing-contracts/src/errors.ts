/** Canonical Individual Premium billing API error codes → athlete-facing copy. */
export const BILLING_ERROR_MESSAGES: Readonly<Record<string, string>> = {
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

export type BillingApiErrorLike = {
  code?: string;
  message?: string;
};

/**
 * Resolve athlete-facing copy from a known billing code, else the API message, else fallback.
 * Platform wrappers supply ApiError detection and category-specific overrides.
 */
export function resolveBillingErrorMessage(
  error: BillingApiErrorLike | null | undefined,
  fallback: string,
): string {
  if (error?.code && BILLING_ERROR_MESSAGES[error.code]) {
    return BILLING_ERROR_MESSAGES[error.code];
  }
  if (error?.message) {
    return error.message;
  }
  return fallback;
}

export function isAccountNotFoundCode(code: string | undefined): boolean {
  return code === 'ACCOUNT_NOT_FOUND';
}
