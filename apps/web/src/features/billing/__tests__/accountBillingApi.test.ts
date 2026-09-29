import { afterEach, describe, expect, it, vi } from 'vitest';

import type { ApiClient } from '@/core/api/apiClient';
import {
  clearPendingAccountCheckout,
  createAccountCheckoutSession,
  fetchAccountBillingStatus,
  readPendingAccountCheckout,
  rememberPendingAccountCheckout,
  syncAccountSubscription,
} from '@/features/billing/api/accountBillingApi';

function clientWith(axios: { post?: unknown; get?: unknown }): ApiClient {
  return { axios } as ApiClient;
}

describe('accountBillingApi', () => {
  afterEach(() => {
    clearPendingAccountCheckout();
  });

  it('posts only planKey cadence and requestId then returns checkout fields', async () => {
    const post = vi.fn().mockResolvedValue({
      data: {
        subscriptionId: '11111111-2222-3333-4444-555555555555',
        checkoutSessionId: 'cs_1',
        checkoutUrl: 'https://checkout.stripe.test/cs_1',
      },
    });

    const result = await createAccountCheckoutSession(clientWith({ post }), {
      requestId: '11111111-2222-3333-4444-555555555555',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
    });

    expect(post).toHaveBeenCalledWith('/api/v1/billing/account/checkout-sessions', {
      requestId: '11111111-2222-3333-4444-555555555555',
      planKey: 'INDIVIDUAL_PREMIUM',
      cadence: 'MONTHLY',
    });
    expect(result.checkoutUrl).toBe('https://checkout.stripe.test/cs_1');
    expect(result.subscriptionId).toBe('11111111-2222-3333-4444-555555555555');
    expect(result.checkoutSessionId).toBe('cs_1');
  });

  it('fetches account billing status without provider identifiers', async () => {
    const get = vi.fn().mockResolvedValue({
      data: {
        subscriptionId: '11111111-2222-3333-4444-555555555555',
        planKey: 'INDIVIDUAL_PREMIUM',
        cadence: 'ANNUAL',
        lifecycleState: 'ACTIVE',
        trialEndsAt: null,
        currentPeriodEndsAt: '2026-10-01T00:00:00Z',
        graceEndsAt: null,
      },
    });

    const status = await fetchAccountBillingStatus(clientWith({ get }));

    expect(get).toHaveBeenCalledWith('/api/v1/billing/account');
    expect(status.lifecycleState).toBe('ACTIVE');
    expect(status.planKey).toBe('INDIVIDUAL_PREMIUM');
  });

  it('posts sync with checkoutSessionId only', async () => {
    const post = vi.fn().mockResolvedValue({
      data: {
        subscriptionId: '11111111-2222-3333-4444-555555555555',
        planKey: 'INDIVIDUAL_PREMIUM',
        cadence: 'MONTHLY',
        lifecycleState: 'ACTIVE',
        trialEndsAt: null,
        currentPeriodEndsAt: '2026-10-01T00:00:00Z',
        graceEndsAt: null,
      },
    });

    const status = await syncAccountSubscription(
      clientWith({ post }),
      '11111111-2222-3333-4444-555555555555',
      'cs_test_1',
    );

    expect(post).toHaveBeenCalledWith(
      '/api/v1/billing/account/subscriptions/11111111-2222-3333-4444-555555555555/sync',
      { checkoutSessionId: 'cs_test_1' },
    );
    expect(status.lifecycleState).toBe('ACTIVE');
  });

  it('remembers and clears pending checkout in sessionStorage', () => {
    rememberPendingAccountCheckout({
      subscriptionId: 'sub-1',
      checkoutSessionId: 'cs_1',
    });
    expect(readPendingAccountCheckout()).toEqual({
      subscriptionId: 'sub-1',
      checkoutSessionId: 'cs_1',
    });
    clearPendingAccountCheckout();
    expect(readPendingAccountCheckout()).toBeNull();
  });
});
