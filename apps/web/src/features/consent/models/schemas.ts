import { z } from 'zod';

export const CONSENT_SCOPES = [
  'AVAILABILITY',
  'READINESS_CATEGORY',
  'READINESS_SCORE',
  'LIMITING_DIMENSIONS',
  'RECOVERY_CHECK_IN_DETAIL',
  'TRAINING_ADHERENCE',
  'PERFORMANCE_HISTORY',
  'TRAINING_COLLABORATION',
  'EXPORT',
] as const;

export type ConsentScope = (typeof CONSENT_SCOPES)[number];

export const consentScopeSchema = z.enum(CONSENT_SCOPES);

export const consentGrantStatusSchema = z.enum(['ACTIVE', 'REVOKED']);

export type ConsentGrantStatus = z.infer<typeof consentGrantStatusSchema>;

export const consentGrantSchema = z.object({
  id: z.string().min(1),
  athleteId: z.string().min(1),
  teamId: z.string().min(1),
  organizationId: z.string().min(1),
  teamMembershipId: z.string().min(1),
  scopes: z.array(consentScopeSchema).min(1),
  status: consentGrantStatusSchema,
  createdAt: z.string().min(1),
  revokedAt: z.string().nullable().optional(),
  updatedAt: z.string().min(1),
  version: z.number().int(),
  teamName: z.string().min(1).nullable().optional(),
  organizationName: z.string().min(1).nullable().optional(),
});

export type ConsentGrant = z.infer<typeof consentGrantSchema>;

export const consentGrantListSchema = z.array(consentGrantSchema);

export const createConsentGrantRequestSchema = z.object({
  teamId: z.string().uuid(),
  scopes: z.array(consentScopeSchema).min(1),
});

export type CreateConsentGrantRequest = z.infer<typeof createConsentGrantRequestSchema>;
