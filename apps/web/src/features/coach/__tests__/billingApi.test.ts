import { describe, expect, it, vi } from 'vitest';

import type { ApiClient } from '@/core/api/apiClient';
import {
  cancelOrganizationRenewal,
  changeOrganizationPlan,
  createOrganizationCheckoutSession,
  createOrganizationPortalSession,
  fetchOrganizationBillingStatus,
  fetchOrganizationCapacity,
  reactivateOrganizationSubscription,
} from '@/features/coach/api/billingApi';

function clientWith(axios: { post?: unknown; get?: unknown }): ApiClient {
  return { axios } as ApiClient;
}

describe('billingApi', () => {
  it('posts only planKey cadence and requestId then returns checkoutUrl', async () => {
    const post = vi.fn().mockResolvedValue({
      data: {
        subscriptionId: '11111111-2222-3333-4444-555555555555',
        checkoutSessionId: 'cs_1',
        checkoutUrl: 'https://checkout.stripe.test/cs_1',
      },
    });

    const result = await createOrganizationCheckoutSession(clientWith({ post }), 'org-1', {
      requestId: '11111111-2222-3333-4444-555555555555',
      planKey: 'ORG_BAND_25',
      cadence: 'MONTHLY',
    });

    expect(post).toHaveBeenCalledWith(
      '/api/v1/billing/organizations/org-1/checkout-sessions',
      {
        requestId: '11111111-2222-3333-4444-555555555555',
        planKey: 'ORG_BAND_25',
        cadence: 'MONTHLY',
      },
    );
    expect(result.checkoutUrl).toBe('https://checkout.stripe.test/cs_1');
  });

  it('fetches organization billing status', async () => {
    const get = vi.fn().mockResolvedValue({
      data: {
        subscriptionId: '11111111-2222-3333-4444-555555555555',
        planKey: 'ORG_BAND_25',
        cadence: 'MONTHLY',
        lifecycleState: 'PENDING',
        trialEndsAt: null,
        currentPeriodEndsAt: null,
      },
    });

    const status = await fetchOrganizationBillingStatus(clientWith({ get }), 'org-1');

    expect(get).toHaveBeenCalledWith('/api/v1/billing/organizations/org-1');
    expect(status.lifecycleState).toBe('PENDING');
    expect(status.planKey).toBe('ORG_BAND_25');
  });

  it('fetches organization capacity snapshot', async () => {
    const get = vi.fn().mockResolvedValue({
      data: {
        activeAthleteCount: 23,
        bandCapacity: 25,
        remainingCapacity: 2,
        atCapacity: false,
        overCapacity: false,
      },
    });

    const snapshot = await fetchOrganizationCapacity(clientWith({ get }), 'org-1');

    expect(get).toHaveBeenCalledWith('/api/v1/billing/organizations/org-1/capacity');
    expect(snapshot.activeAthleteCount).toBe(23);
    expect(snapshot.bandCapacity).toBe(25);
  });

  it('posts portal, plan change, cancel, and reactivate without provider identifiers', async () => {
    const post = vi.fn()
      .mockResolvedValueOnce({ data: { url: 'https://billing.stripe.test/portal/org-1' } })
      .mockResolvedValue({
        data: {
          subscriptionId: '11111111-2222-3333-4444-555555555555',
          planKey: 'ORG_BAND_75',
          cadence: 'ANNUAL',
          lifecycleState: 'ACTIVE',
          trialEndsAt: null,
          currentPeriodEndsAt: '2026-10-01T00:00:00Z',
        },
      });
    const client = clientWith({ post });
    const requestId = 'aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee';
    const subscriptionId = '11111111-2222-3333-4444-555555555555';

    const portal = await createOrganizationPortalSession(client, 'org-1');
    const changed = await changeOrganizationPlan(client, 'org-1', subscriptionId, {
      requestId,
      targetPlanKey: 'ORG_BAND_75',
      targetCadence: 'ANNUAL',
    });
    const cancelled = await cancelOrganizationRenewal(client, 'org-1', subscriptionId, requestId);
    const reactivated = await reactivateOrganizationSubscription(client, 'org-1', subscriptionId, requestId);

    expect(portal.url).toBe('https://billing.stripe.test/portal/org-1');
    expect(changed.planKey).toBe('ORG_BAND_75');
    expect(cancelled.lifecycleState).toBe('ACTIVE');
    expect(reactivated.cadence).toBe('ANNUAL');
    expect(post).toHaveBeenNthCalledWith(1, '/api/v1/billing/organizations/org-1/portal-sessions');
    expect(post).toHaveBeenNthCalledWith(
      2,
      '/api/v1/billing/organizations/org-1/subscriptions/11111111-2222-3333-4444-555555555555/plan-changes',
      {
        requestId,
        targetPlanKey: 'ORG_BAND_75',
        targetCadence: 'ANNUAL',
      },
    );
    expect(post).toHaveBeenNthCalledWith(
      3,
      '/api/v1/billing/organizations/org-1/subscriptions/11111111-2222-3333-4444-555555555555/cancel',
      { requestId },
    );
    expect(post).toHaveBeenNthCalledWith(
      4,
      '/api/v1/billing/organizations/org-1/subscriptions/11111111-2222-3333-4444-555555555555/reactivate',
      { requestId },
    );
  });
});
