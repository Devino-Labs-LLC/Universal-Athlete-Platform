import {
  APPLE_SUBSCRIPTIONS_URL,
  GOOGLE_PLAY_SUBSCRIPTIONS_URL,
  BILLING_ERROR_MESSAGES,
  cadenceLabel,
  formatUsd,
  isAccountNotFoundCode,
  isStripeManagedChannel,
  premiumOriginLabel,
  resolveBillingErrorMessage,
  storeManagementLabel,
  storeManagementUrl,
  accountBillingStatusSchema,
  checkoutSessionResponseSchema,
  individualPremiumCatalog,
} from '@uap/billing-contracts';

describe('@uap/billing-contracts', () => {
  it('exposes catalog prices and origin labels', () => {
    expect(individualPremiumCatalog.monthlyUsd).toBe(9.99);
    expect(premiumOriginLabel('STRIPE')).toBe('Stripe');
    expect(premiumOriginLabel('APPLE_APP_STORE')).toBe('App Store');
    expect(premiumOriginLabel('GOOGLE_PLAY')).toBe('Google Play');
    expect(isStripeManagedChannel('STRIPE_CUSTOMER_PORTAL')).toBe(true);
    expect(isStripeManagedChannel('APPLE_APP_STORE')).toBe(false);
  });

  it('maps store management urls and labels', () => {
    expect(storeManagementUrl('APPLE_APP_STORE')).toBe(APPLE_SUBSCRIPTIONS_URL);
    expect(storeManagementUrl('GOOGLE_PLAY')).toBe(GOOGLE_PLAY_SUBSCRIPTIONS_URL);
    expect(storeManagementUrl('STRIPE_CUSTOMER_PORTAL')).toBeNull();
    expect(storeManagementLabel('APPLE_APP_STORE')).toBe('Manage in App Store');
    expect(storeManagementLabel('GOOGLE_PLAY')).toBe('Manage in Google Play');
    expect(storeManagementLabel('STRIPE_CUSTOMER_PORTAL')).toBe(
      'Manage payment method and invoices',
    );
    expect(
      storeManagementLabel('STRIPE_CUSTOMER_PORTAL', {
        stripePortalLabel: 'Manage on the web',
      }),
    ).toBe('Manage on the web');
  });

  it('formats money and cadence labels', () => {
    expect(formatUsd(9.99)).toContain('9.99');
    expect(cadenceLabel('MONTHLY')).toBe('Monthly');
    expect(cadenceLabel('ANNUAL')).toBe('Annual');
    expect(cadenceLabel(null)).toBe('Cadence unavailable');
  });

  it('resolves billing error messages and account-not-found codes', () => {
    expect(BILLING_ERROR_MESSAGES.BILLING_PROVIDER_UNAVAILABLE).toContain('unavailable');
    expect(
      resolveBillingErrorMessage({ code: 'BILLING_CHECKOUT_IN_PROGRESS' }, 'fallback'),
    ).toBe('Checkout is already in progress.');
    expect(resolveBillingErrorMessage({ message: 'raw' }, 'fallback')).toBe('raw');
    expect(resolveBillingErrorMessage({}, 'fallback')).toBe('fallback');
    expect(isAccountNotFoundCode('ACCOUNT_NOT_FOUND')).toBe(true);
    expect(isAccountNotFoundCode('OTHER')).toBe(false);
  });

  it('parses account billing status and checkout schemas', () => {
    expect(
      accountBillingStatusSchema.parse({
        subscriptionId: 'sub_1',
        provider: 'STRIPE',
        managementChannel: 'STRIPE_CUSTOMER_PORTAL',
        planKey: 'INDIVIDUAL_PREMIUM',
        cadence: 'MONTHLY',
        lifecycleState: 'ACTIVE',
        trialEndsAt: null,
        currentPeriodEndsAt: null,
        graceEndsAt: null,
      }).subscriptionId,
    ).toBe('sub_1');
    expect(
      checkoutSessionResponseSchema.parse({
        subscriptionId: 'sub_1',
        checkoutSessionId: 'cs_1',
        checkoutUrl: 'https://checkout.example.com/cs_1',
      }).checkoutSessionId,
    ).toBe('cs_1');
  });
});
