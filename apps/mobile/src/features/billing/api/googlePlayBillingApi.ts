import { z } from 'zod';

import { ApiClient } from '@/src/core/api/apiClient';

const GOOGLE_PLAY_PURCHASES_PATH = '/api/v1/billing/account/google-play/purchases';

const googlePlaySubscriptionResultSchema = z.object({
  subscriptionId: z.string().uuid(),
  provider: z.literal('GOOGLE_PLAY'),
  planKey: z.literal('INDIVIDUAL_PREMIUM'),
  cadence: z.enum(['MONTHLY', 'ANNUAL']).nullable(),
  lifecycleState: z.enum([
    'PENDING',
    'TRIALING',
    'ACTIVE',
    'PAST_DUE',
    'GRACE_PERIOD',
    'CANCEL_AT_PERIOD_END',
    'EXPIRED',
  ]),
  trialEndsAt: z.string().nullable(),
  currentPeriodEndsAt: z.string().nullable(),
  graceEndsAt: z.string().nullable(),
});

export type GooglePlaySubscriptionResult = z.infer<typeof googlePlaySubscriptionResultSchema>;

/**
 * Thin G3 client: posts a Play Billing purchaseToken + productId to the server validate/restore endpoint.
 * Full purchase / restore UX belongs to H1.
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
