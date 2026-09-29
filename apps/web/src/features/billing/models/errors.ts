import { isApiError } from '@/core/api/errors';
import {
  BILLING_ERROR_MESSAGES,
  isAccountNotFoundCode,
  resolveBillingErrorMessage,
} from '@uap/billing-contracts';

export function accountBillingErrorMessage(
  error: unknown,
  fallback = 'Unable to update billing.',
): string {
  if (isApiError(error)) {
    if (error.code && BILLING_ERROR_MESSAGES[error.code]) {
      return BILLING_ERROR_MESSAGES[error.code];
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
    return resolveBillingErrorMessage(error, fallback);
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

/** Enabled Stripe with no Account subscription. */
export function isAccountBillingMissing(error: unknown): boolean {
  return isApiError(error) && error.category === 'NOT_FOUND' && isAccountNotFoundCode(error.code);
}

/**
 * Stripe billing controllers are absent when disabled — Spring returns a generic 404.
 * Treat non-ACCOUNT_NOT_FOUND not-found as “billing unavailable” so the page degrades.
 */
export function isAccountBillingUnavailable(error: unknown): boolean {
  return isApiError(error) && error.category === 'NOT_FOUND' && !isAccountNotFoundCode(error.code);
}
