import { isApiError } from '@/core/api/errors';

const billingMessages: Record<string, string> = {
  ORGANIZATION_PLAN_CAPACITY_CONFLICT:
    'The selected plan does not cover the current active athletes.',
  BILLING_LIFECYCLE_CONFLICT: 'This subscription cannot be changed in its current state.',
  BILLING_SUBSCRIPTION_NOT_MANAGEABLE: 'This subscription cannot be managed.',
  BILLING_SUBSCRIPTION_STATE_CONFLICT:
    'Organization billing needs attention before it can be managed.',
  BILLING_REQUEST_CONFLICT: 'That billing request was already used for a different change.',
  BILLING_PROVIDER_PRICE_REJECTED: 'The billing provider could not apply that plan.',
  BILLING_PAYMENT_NOT_APPLIED:
    'The plan change was not applied because payment could not be collected. Your current plan is unchanged.',
  BILLING_PROVIDER_UNAVAILABLE: 'Billing is temporarily unavailable. Try again.',
  BILLING_CHECKOUT_IN_PROGRESS: 'Checkout is already in progress.',
};

export function coachErrorMessage(error: unknown, fallback = 'Something went wrong.'): string {
  if (isApiError(error)) {
    if (error.code && billingMessages[error.code]) {
      return billingMessages[error.code];
    }
    if (error.category === 'NOT_FOUND') {
      return 'This team or athlete is unavailable.';
    }
    if (error.category === 'UNAUTHORIZED') {
      return 'Your session expired. Sign in again to continue.';
    }
    if (error.category === 'COMMERCIAL_ENTITLEMENT' || error.code === 'COMMERCIAL_ENTITLEMENT_REQUIRED') {
      return 'This organization does not currently have access to this capability.';
    }
    return error.message || fallback;
  }
  if (error instanceof Error) {
    return error.message;
  }
  return fallback;
}

export function isCoachNotFoundError(error: unknown): boolean {
  return isApiError(error) && error.category === 'NOT_FOUND';
}

export function isCoachUnauthorizedError(error: unknown): boolean {
  return isApiError(error) && error.category === 'UNAUTHORIZED';
}
