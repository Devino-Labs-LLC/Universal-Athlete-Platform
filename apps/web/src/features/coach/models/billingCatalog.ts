import { z } from 'zod';

export const organizationPlanKeySchema = z.enum(['ORG_BAND_25', 'ORG_BAND_75', 'ORG_BAND_250']);
export const billingCadenceSchema = z.enum(['MONTHLY', 'ANNUAL']);
export const organizationSubscriptionStateSchema = z.enum([
  'PENDING',
  'TRIALING',
  'ACTIVE',
  'PAST_DUE',
  'GRACE_PERIOD',
  'CANCEL_AT_PERIOD_END',
  'EXPIRED',
]);

export const organizationCatalog = [
  {
    planKey: 'ORG_BAND_25' as const,
    name: 'Starter',
    capacity: 25,
    monthlyUsd: 49,
    annualUsd: 490,
  },
  {
    planKey: 'ORG_BAND_75' as const,
    name: 'Team',
    capacity: 75,
    monthlyUsd: 99,
    annualUsd: 990,
  },
  {
    planKey: 'ORG_BAND_250' as const,
    name: 'Organization',
    capacity: 250,
    monthlyUsd: 149,
    annualUsd: 1490,
  },
] as const;

export const checkoutSessionResponseSchema = z.object({
  subscriptionId: z.string().min(1),
  checkoutSessionId: z.string().min(1),
  checkoutUrl: z.string().url(),
});

export const organizationBillingStatusSchema = z.object({
  subscriptionId: z.string().min(1),
  planKey: organizationPlanKeySchema,
  cadence: billingCadenceSchema.nullable(),
  lifecycleState: organizationSubscriptionStateSchema,
  trialEndsAt: z.string().nullable(),
  currentPeriodEndsAt: z.string().nullable(),
});

export type OrganizationBillingStatus = z.infer<typeof organizationBillingStatusSchema>;

export const organizationCapacitySnapshotSchema = z.object({
  activeAthleteCount: z.number().int().nonnegative(),
  bandCapacity: z.number().int().positive().nullable().optional(),
  remainingCapacity: z.number().int().nonnegative().nullable().optional(),
  atCapacity: z.boolean(),
  overCapacity: z.boolean(),
});

export type OrganizationCapacitySnapshot = z.infer<typeof organizationCapacitySnapshotSchema>;
