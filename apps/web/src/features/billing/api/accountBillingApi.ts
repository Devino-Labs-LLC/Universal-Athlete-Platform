import type { ApiClient } from '@/core/api/apiClient';

import {
  accountBillingStatusSchema,
  checkoutSessionResponseSchema,
  type AccountBillingStatus,
  type CheckoutSessionResponse,
} from '@/features/billing/models/accountBilling';

const PENDING_CHECKOUT_STORAGE_KEY = 'uap.accountBilling.pendingCheckout';

export type PendingAccountCheckout = {
  subscriptionId: string;
  checkoutSessionId: string;
};

export function rememberPendingAccountCheckout(pending: PendingAccountCheckout): void {
  try {
    sessionStorage.setItem(PENDING_CHECKOUT_STORAGE_KEY, JSON.stringify(pending));
  } catch {
    // sessionStorage may be unavailable; success page can still use query params.
  }
}

export function readPendingAccountCheckout(): PendingAccountCheckout | null {
  try {
    const raw = sessionStorage.getItem(PENDING_CHECKOUT_STORAGE_KEY);
    if (!raw) {
      return null;
    }
    const parsed = JSON.parse(raw) as Partial<PendingAccountCheckout>;
    if (
      typeof parsed.subscriptionId === 'string' &&
      parsed.subscriptionId.length > 0 &&
      typeof parsed.checkoutSessionId === 'string' &&
      parsed.checkoutSessionId.length > 0
    ) {
      return {
        subscriptionId: parsed.subscriptionId,
        checkoutSessionId: parsed.checkoutSessionId,
      };
    }
    return null;
  } catch {
    return null;
  }
}

export function clearPendingAccountCheckout(): void {
  try {
    sessionStorage.removeItem(PENDING_CHECKOUT_STORAGE_KEY);
  } catch {
    // ignore
  }
}

export async function fetchAccountBillingStatus(client: ApiClient): Promise<AccountBillingStatus> {
  const response = await client.axios.get('/api/v1/billing/account');
  return accountBillingStatusSchema.parse(response.data);
}

export async function createAccountCheckoutSession(
  client: ApiClient,
  input: { requestId: string; planKey: 'INDIVIDUAL_PREMIUM'; cadence: 'MONTHLY' | 'ANNUAL' },
): Promise<CheckoutSessionResponse> {
  const response = await client.axios.post('/api/v1/billing/account/checkout-sessions', {
    requestId: input.requestId,
    planKey: input.planKey,
    cadence: input.cadence,
  });
  return checkoutSessionResponseSchema.parse(response.data);
}

export async function syncAccountSubscription(
  client: ApiClient,
  subscriptionId: string,
  checkoutSessionId: string,
): Promise<AccountBillingStatus> {
  const response = await client.axios.post(
    `/api/v1/billing/account/subscriptions/${subscriptionId}/sync`,
    { checkoutSessionId },
  );
  return accountBillingStatusSchema.parse(response.data);
}
