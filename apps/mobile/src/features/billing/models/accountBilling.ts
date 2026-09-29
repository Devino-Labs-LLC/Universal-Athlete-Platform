export {
  APPLE_SUBSCRIPTIONS_URL,
  GOOGLE_PLAY_SUBSCRIPTIONS_URL,
  accountBillingStatusSchema,
  billingCadenceSchema,
  billingProviderSchema,
  cadenceLabel,
  formatUsd,
  individualManagementChannelSchema,
  individualPlanKeySchema,
  individualPremiumCatalog,
  accountSubscriptionStateSchema,
  isStripeManagedChannel,
  premiumOriginLabel,
  storeManagementUrl,
} from '@uap/billing-contracts';

export type {
  AccountBillingStatus,
  BillingCadence,
  BillingProvider,
  IndividualManagementChannel,
} from '@uap/billing-contracts';

import {
  storeManagementLabel as sharedStoreManagementLabel,
  type IndividualManagementChannel,
} from '@uap/billing-contracts';

/** Mobile copy for Stripe Customer Portal (manage on web). */
export function storeManagementLabel(channel: IndividualManagementChannel): string {
  return sharedStoreManagementLabel(channel, {
    stripePortalLabel: 'Manage on the web',
  });
}
