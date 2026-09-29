import { validateOrRestoreGooglePlayPurchase } from '@/src/features/billing/api/googlePlayBillingApi';

describe('googlePlayBillingApi', () => {
  it('posts purchaseToken and productId to the Google Play validate/restore endpoint', async () => {
    const post = jest.fn().mockResolvedValue({
      data: {
        subscriptionId: 'aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee',
        provider: 'GOOGLE_PLAY',
        planKey: 'INDIVIDUAL_PREMIUM',
        cadence: 'MONTHLY',
        lifecycleState: 'ACTIVE',
        trialEndsAt: null,
        currentPeriodEndsAt: '2026-10-28T18:00:00Z',
        graceEndsAt: null,
      },
    });
    const client = { axios: { post } };

    const result = await validateOrRestoreGooglePlayPurchase(
      client as never,
      'google-play-purchase-token-abc123xyz',
      'premium.monthly',
    );

    expect(post).toHaveBeenCalledWith('/api/v1/billing/account/google-play/purchases', {
      purchaseToken: 'google-play-purchase-token-abc123xyz',
      productId: 'premium.monthly',
    });
    expect(result.provider).toBe('GOOGLE_PLAY');
    expect(result.lifecycleState).toBe('ACTIVE');
  });
});
