import { z } from 'zod';

/** Canonical Slice C consent scopes — none are preselected in grant UX. */
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

export const CONSENT_GRANT_STATUSES = ['ACTIVE', 'REVOKED'] as const;

export type ConsentGrantStatus = (typeof CONSENT_GRANT_STATUSES)[number];

export const consentGrantStatusSchema = z.enum(CONSENT_GRANT_STATUSES);

/** GET/POST /api/v1/athletes/me/consents item — mirrors ConsentGrantResponse. */
export const consentGrantSchema = z.object({
  id: z.string(),
  athleteId: z.string(),
  teamId: z.string(),
  organizationId: z.string(),
  teamMembershipId: z.string(),
  scopes: z.array(consentScopeSchema),
  status: consentGrantStatusSchema,
  createdAt: z.string(),
  revokedAt: z.string().nullable().optional(),
  updatedAt: z.string(),
  version: z.number(),
  teamName: z.string().nullable().optional(),
  organizationName: z.string().nullable().optional(),
});

export type ConsentGrant = z.infer<typeof consentGrantSchema>;

export const consentGrantsSchema = z.array(consentGrantSchema);

export const createConsentGrantRequestSchema = z.object({
  teamId: z.string().uuid(),
  scopes: z.array(consentScopeSchema).min(1),
});

export type CreateConsentGrantRequest = z.infer<typeof createConsentGrantRequestSchema>;

/** GET /api/v1/athletes/me/teams item — mirrors MyAthleteTeamResponse. */
export const myAthleteTeamMembershipSchema = z.object({
  membershipId: z.string(),
  teamId: z.string(),
  teamName: z.string(),
  organizationId: z.string(),
  organizationName: z.string(),
  athleteId: z.string(),
});

export type MyAthleteTeamMembership = z.infer<typeof myAthleteTeamMembershipSchema>;

export const myAthleteTeamMembershipsSchema = z.array(myAthleteTeamMembershipSchema);
