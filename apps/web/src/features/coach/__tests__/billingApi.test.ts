import { describe, expect, it, vi } from 'vitest';

import type { ApiClient } from '@/core/api/apiClient';
import {
  createOrganizationCheckoutSession,
  fetchOrganizationBillingStatus,
  fetchOrganizationCapacity,
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
});
