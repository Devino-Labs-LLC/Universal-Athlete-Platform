import { validateOrRestoreAppleTransaction } from '@/src/features/billing/api/appleBillingApi';

describe('appleBillingApi', () => {
  it('posts signedTransactionInfo to the Apple validate/restore endpoint', async () => {
    const post = jest.fn().mockResolvedValue({
      data: {
        subscriptionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
        provider: 'APPLE_APP_STORE',
        planKey: 'INDIVIDUAL_PREMIUM',
        cadence: 'MONTHLY',
        lifecycleState: 'ACTIVE',
        trialEndsAt: null,
        currentPeriodEndsAt: '2026-10-28T18:00:00Z',
        graceEndsAt: null,
      },
    });
    const client = { axios: { post } };

    const result = await validateOrRestoreAppleTransaction(
      client as never,
      'header.payload.signature',
    );

    expect(post).toHaveBeenCalledWith('/api/v1/billing/account/apple/transactions', {
      signedTransactionInfo: 'header.payload.signature',
    });
    expect(result.provider).toBe('APPLE_APP_STORE');
    expect(result.lifecycleState).toBe('ACTIVE');
  });
});
