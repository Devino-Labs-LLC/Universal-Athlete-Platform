import { z } from 'zod';

import { ApiClient } from '@/src/core/api/apiClient';
import { accountBillingStatusSchema } from '@/src/features/billing/models/accountBilling';

const APPLE_TRANSACTIONS_PATH = '/api/v1/billing/account/apple/transactions';

const appleSubscriptionResultSchema = accountBillingStatusSchema.extend({
  provider: z.literal('APPLE_APP_STORE'),
  managementChannel: z.literal('APPLE_APP_STORE'),
});

export type AppleSubscriptionResult = z.infer<typeof appleSubscriptionResultSchema>;

/**
 * Thin G2 client: posts a StoreKit signed transaction to the server validate/restore endpoint.
 * Purchase / restore UX entry points live in H1 PremiumBillingScreen.
 */
export async function validateOrRestoreAppleTransaction(
  client: ApiClient,
  signedTransactionInfo: string,
): Promise<AppleSubscriptionResult> {
  const response = await client.axios.post(APPLE_TRANSACTIONS_PATH, {
    signedTransactionInfo,
  });
  return appleSubscriptionResultSchema.parse(response.data);
}
