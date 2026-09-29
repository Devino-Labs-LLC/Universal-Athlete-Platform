export {
  APPLE_SUBSCRIPTIONS_URL,
  GOOGLE_PLAY_SUBSCRIPTIONS_URL,
  accountBillingStatusSchema,
  billingCadenceSchema,
  billingProviderSchema,
  checkoutSessionResponseSchema,
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
  BillingProvider,
  CheckoutSessionResponse,
  IndividualManagementChannel,
} from '@uap/billing-contracts';

import {
  storeManagementLabel as sharedStoreManagementLabel,
  type IndividualManagementChannel,
} from '@uap/billing-contracts';

/** Web copy for Stripe Customer Portal. */
export function storeManagementLabel(channel: IndividualManagementChannel): string {
  return sharedStoreManagementLabel(channel);
}
