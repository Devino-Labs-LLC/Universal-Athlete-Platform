import { z } from 'zod';

export const individualPlanKeySchema = z.literal('INDIVIDUAL_PREMIUM');
export const billingCadenceSchema = z.enum(['MONTHLY', 'ANNUAL']);
export const accountSubscriptionStateSchema = z.enum([
  'PENDING',
  'TRIALING',
  'ACTIVE',
  'PAST_DUE',
  'GRACE_PERIOD',
  'CANCEL_AT_PERIOD_END',
  'EXPIRED',
]);

export const billingProviderSchema = z.enum(['STRIPE', 'APPLE_APP_STORE', 'GOOGLE_PLAY']);
export const individualManagementChannelSchema = z.enum([
  'STRIPE_CUSTOMER_PORTAL',
  'APPLE_APP_STORE',
  'GOOGLE_PLAY',
]);

/** Locked Individual Premium display prices (tax-exclusive). Not store product IDs. */
export const individualPremiumCatalog = {
  planKey: 'INDIVIDUAL_PREMIUM' as const,
  name: 'Individual Premium',
  monthlyUsd: 9.99,
  annualUsd: 99.99,
} as const;

export const accountBillingStatusSchema = z.object({
  subscriptionId: z.string().min(1),
  provider: billingProviderSchema,
  managementChannel: individualManagementChannelSchema,
  planKey: individualPlanKeySchema,
  cadence: billingCadenceSchema.nullable(),
  lifecycleState: accountSubscriptionStateSchema,
  trialEndsAt: z.string().nullable(),
  currentPeriodEndsAt: z.string().nullable(),
  graceEndsAt: z.string().nullable(),
});

export type AccountBillingStatus = z.infer<typeof accountBillingStatusSchema>;
export type BillingProvider = z.infer<typeof billingProviderSchema>;
export type IndividualManagementChannel = z.infer<typeof individualManagementChannelSchema>;

export function premiumOriginLabel(provider: BillingProvider): string {
  switch (provider) {
    case 'STRIPE':
      return 'Stripe';
    case 'APPLE_APP_STORE':
      return 'App Store';
    case 'GOOGLE_PLAY':
      return 'Google Play';
  }
}

export function isStripeManagedChannel(channel: IndividualManagementChannel): boolean {
  return channel === 'STRIPE_CUSTOMER_PORTAL';
}

export const APPLE_SUBSCRIPTIONS_URL = 'https://apps.apple.com/account/subscriptions';
export const GOOGLE_PLAY_SUBSCRIPTIONS_URL =
  'https://play.google.com/store/account/subscriptions';

export function storeManagementUrl(
  channel: IndividualManagementChannel,
): string | null {
  switch (channel) {
    case 'APPLE_APP_STORE':
      return APPLE_SUBSCRIPTIONS_URL;
    case 'GOOGLE_PLAY':
      return GOOGLE_PLAY_SUBSCRIPTIONS_URL;
    case 'STRIPE_CUSTOMER_PORTAL':
      return null;
  }
}

export function storeManagementLabel(channel: IndividualManagementChannel): string {
  switch (channel) {
    case 'APPLE_APP_STORE':
      return 'Manage in App Store';
    case 'GOOGLE_PLAY':
      return 'Manage in Google Play';
    case 'STRIPE_CUSTOMER_PORTAL':
      return 'Manage on the web';
  }
}

export function formatUsd(amount: number): string {
  return new Intl.NumberFormat('en-US', {
    style: 'currency',
    currency: 'USD',
  }).format(amount);
}

export function cadenceLabel(cadence: 'MONTHLY' | 'ANNUAL' | null): string {
  if (cadence === 'ANNUAL') {
    return 'Annual';
  }
  if (cadence === 'MONTHLY') {
    return 'Monthly';
  }
  return 'Cadence unavailable';
}
