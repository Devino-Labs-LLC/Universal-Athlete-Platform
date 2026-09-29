import { afterEach, describe, expect, it, vi } from 'vitest';

import type { ApiClient } from '@/core/api/apiClient';
import {
  cancelAccountRenewal,
  clearPendingAccountCheckout,
  createAccountCheckoutSession,
  createAccountPortalSession,
  fetchAccountBillingStatus,
  readPendingAccountCheckout,
  reactivateAccountSubscription,
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

  it('fetches account billing status with provider and management channel', async () => {
    const get = vi.fn().mockResolvedValue({
      data: {
        subscriptionId: '11111111-2222-3333-4444-555555555555',
        provider: 'STRIPE',
        managementChannel: 'STRIPE_CUSTOMER_PORTAL',
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
    expect(status.provider).toBe('STRIPE');
    expect(status.managementChannel).toBe('STRIPE_CUSTOMER_PORTAL');
  });

  it('posts sync with checkoutSessionId only', async () => {
    const post = vi.fn().mockResolvedValue({
      data: {
        subscriptionId: '11111111-2222-3333-4444-555555555555',
        provider: 'STRIPE',
        managementChannel: 'STRIPE_CUSTOMER_PORTAL',
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

  it('posts portal cancel and reactivate without price identifiers', async () => {
    const post = vi
      .fn()
      .mockResolvedValueOnce({
        data: { url: 'https://billing.stripe.test/portal/account' },
      })
      .mockResolvedValueOnce({
        data: {
          subscriptionId: '11111111-2222-3333-4444-555555555555',
          provider: 'STRIPE',
          managementChannel: 'STRIPE_CUSTOMER_PORTAL',
          planKey: 'INDIVIDUAL_PREMIUM',
          cadence: 'MONTHLY',
          lifecycleState: 'CANCEL_AT_PERIOD_END',
          trialEndsAt: null,
          currentPeriodEndsAt: '2026-10-01T00:00:00Z',
          graceEndsAt: null,
        },
      })
      .mockResolvedValueOnce({
        data: {
          subscriptionId: '11111111-2222-3333-4444-555555555555',
          provider: 'STRIPE',
          managementChannel: 'STRIPE_CUSTOMER_PORTAL',
          planKey: 'INDIVIDUAL_PREMIUM',
          cadence: 'MONTHLY',
          lifecycleState: 'ACTIVE',
          trialEndsAt: null,
          currentPeriodEndsAt: '2026-10-01T00:00:00Z',
          graceEndsAt: null,
        },
      });
    const client = clientWith({ post });
    const requestId = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';
    const subscriptionId = '11111111-2222-3333-4444-555555555555';

    const portal = await createAccountPortalSession(client);
    const cancelled = await cancelAccountRenewal(client, subscriptionId, requestId);
    const reactivated = await reactivateAccountSubscription(client, subscriptionId, requestId);

    expect(portal.url).toBe('https://billing.stripe.test/portal/account');
    expect(cancelled.lifecycleState).toBe('CANCEL_AT_PERIOD_END');
    expect(reactivated.lifecycleState).toBe('ACTIVE');
    expect(post).toHaveBeenNthCalledWith(1, '/api/v1/billing/account/portal-sessions');
    expect(post).toHaveBeenNthCalledWith(
      2,
      '/api/v1/billing/account/subscriptions/11111111-2222-3333-4444-555555555555/cancel',
      { requestId },
    );
    expect(post).toHaveBeenNthCalledWith(
      3,
      '/api/v1/billing/account/subscriptions/11111111-2222-3333-4444-555555555555/reactivate',
      { requestId },
    );
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
