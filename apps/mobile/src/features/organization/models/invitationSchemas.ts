import { z } from 'zod';

export const ORGANIZATION_MEMBERSHIP_ROLES = [
  'ATHLETE',
  'COACH',
  'HEAD_COACH',
  'TEAM_ADMIN',
  'ORG_ADMIN',
  'ORG_OWNER',
] as const;

export type OrganizationMembershipRole = (typeof ORGANIZATION_MEMBERSHIP_ROLES)[number];

export const ORGANIZATION_MEMBERSHIP_STATUSES = ['ACTIVE', 'REMOVED', 'LEFT'] as const;

export type OrganizationMembershipStatus = (typeof ORGANIZATION_MEMBERSHIP_STATUSES)[number];

export const organizationMembershipRoleSchema = z.enum(ORGANIZATION_MEMBERSHIP_ROLES);
export const organizationMembershipStatusSchema = z.enum(ORGANIZATION_MEMBERSHIP_STATUSES);

/** GET /api/v1/me/invitations item — mirrors MyInvitationResponse. */
export const myInvitationSchema = z.object({
  id: z.string(),
  organizationId: z.string(),
  organizationName: z.string(),
  teamId: z.string().nullable().optional(),
  teamName: z.string().nullable().optional(),
  role: organizationMembershipRoleSchema,
  expiresAt: z.string(),
});

export type MyInvitation = z.infer<typeof myInvitationSchema>;

export const myInvitationsSchema = z.array(myInvitationSchema);

/** Mirrors OrganizationMembershipResponse. */
export const organizationMembershipSchema = z.object({
  id: z.string(),
  organizationId: z.string(),
  accountId: z.string(),
  athleteId: z.string().nullable().optional(),
  role: organizationMembershipRoleSchema,
  status: organizationMembershipStatusSchema,
  createdAt: z.string(),
  updatedAt: z.string(),
  version: z.number(),
});

export type OrganizationMembership = z.infer<typeof organizationMembershipSchema>;

/** Mirrors TeamMembershipResponse. */
export const teamMembershipSchema = z.object({
  id: z.string(),
  teamId: z.string(),
  accountId: z.string(),
  athleteId: z.string().nullable().optional(),
  role: organizationMembershipRoleSchema,
  status: organizationMembershipStatusSchema,
  createdAt: z.string(),
  updatedAt: z.string(),
  version: z.number(),
});

export type TeamMembership = z.infer<typeof teamMembershipSchema>;

/** POST …/accept — mirrors AcceptInvitationResponse. */
export const acceptInvitationResponseSchema = z.object({
  organizationMembership: organizationMembershipSchema.nullable().optional(),
  teamMembership: teamMembershipSchema.nullable().optional(),
});

export type AcceptInvitationResponse = z.infer<typeof acceptInvitationResponseSchema>;
