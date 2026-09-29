import { fetchAccountBillingStatus } from '@/src/features/billing/api/accountBillingApi';

describe('accountBillingApi', () => {
  it('fetches GET /billing/account with provider and managementChannel', async () => {
    const get = jest.fn().mockResolvedValue({
      data: {
        subscriptionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
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
    const client = { axios: { get } };

    const status = await fetchAccountBillingStatus(client as never);

    expect(get).toHaveBeenCalledWith('/api/v1/billing/account');
    expect(status.provider).toBe('STRIPE');
    expect(status.managementChannel).toBe('STRIPE_CUSTOMER_PORTAL');
    expect(status.lifecycleState).toBe('ACTIVE');
  });
});
