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

/** Locked Individual Premium display prices (tax-exclusive). Not Stripe Price IDs. */
export const individualPremiumCatalog = {
  planKey: 'INDIVIDUAL_PREMIUM' as const,
  name: 'Individual Premium',
  monthlyUsd: 9.99,
  annualUsd: 99.99,
} as const;

export const checkoutSessionResponseSchema = z.object({
  subscriptionId: z.string().min(1),
  checkoutSessionId: z.string().min(1),
  checkoutUrl: z.string().url(),
});

export type CheckoutSessionResponse = z.infer<typeof checkoutSessionResponseSchema>;

export const accountBillingStatusSchema = z.object({
  subscriptionId: z.string().min(1),
  planKey: individualPlanKeySchema,
  cadence: billingCadenceSchema.nullable(),
  lifecycleState: accountSubscriptionStateSchema,
  trialEndsAt: z.string().nullable(),
  currentPeriodEndsAt: z.string().nullable(),
  graceEndsAt: z.string().nullable(),
});

export type AccountBillingStatus = z.infer<typeof accountBillingStatusSchema>;
