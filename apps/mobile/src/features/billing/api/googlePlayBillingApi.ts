import { z } from 'zod';

import { ApiClient } from '@/src/core/api/apiClient';
import { accountBillingStatusSchema } from '@/src/features/billing/models/accountBilling';

const GOOGLE_PLAY_PURCHASES_PATH = '/api/v1/billing/account/google-play/purchases';

const googlePlaySubscriptionResultSchema = accountBillingStatusSchema.extend({
  provider: z.literal('GOOGLE_PLAY'),
  managementChannel: z.literal('GOOGLE_PLAY'),
});

export type GooglePlaySubscriptionResult = z.infer<typeof googlePlaySubscriptionResultSchema>;

/**
 * Thin G3 client: posts a Play Billing purchaseToken + productId to the server validate/restore endpoint.
 * Purchase / restore UX entry points live in H1 PremiumBillingScreen.
 */
export async function validateOrRestoreGooglePlayPurchase(
  client: ApiClient,
  purchaseToken: string,
  productId: string,
): Promise<GooglePlaySubscriptionResult> {
  const response = await client.axios.post(GOOGLE_PLAY_PURCHASES_PATH, {
    purchaseToken,
    productId,
  });
  return googlePlaySubscriptionResultSchema.parse(response.data);
}
