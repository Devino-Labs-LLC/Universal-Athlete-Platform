import { isApiError } from '@/src/core/api/errors';
import { isAccountNotFoundCode, resolveBillingErrorMessage } from '@uap/billing-contracts';

export function accountBillingErrorMessage(
  error: unknown,
  fallback = 'Unable to update billing.',
): string {
  if (isApiError(error)) {
    return resolveBillingErrorMessage(error, fallback);
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

/** Enabled billing with no Account subscription. */
export function isAccountBillingMissing(error: unknown): boolean {
  return isApiError(error) && error.category === 'notFound' && isAccountNotFoundCode(error.code);
}

/**
 * Billing controllers absent when disabled — generic not-found.
 * Treat non-ACCOUNT_NOT_FOUND not-found as billing unavailable.
 */
export function isAccountBillingUnavailable(error: unknown): boolean {
  return isApiError(error) && error.category === 'notFound' && !isAccountNotFoundCode(error.code);
}
