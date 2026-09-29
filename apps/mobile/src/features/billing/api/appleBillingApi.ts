import { z } from 'zod';

import { ApiClient } from '@/src/core/api/apiClient';

const APPLE_TRANSACTIONS_PATH = '/api/v1/billing/account/apple/transactions';

const appleSubscriptionResultSchema = z.object({
  subscriptionId: z.string().uuid(),
  provider: z.literal('APPLE_APP_STORE'),
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

export type AppleSubscriptionResult = z.infer<typeof appleSubscriptionResultSchema>;

/**
 * Thin G2 client: posts a StoreKit signed transaction to the server validate/restore endpoint.
 * Full purchase / restore UX belongs to H1.
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
