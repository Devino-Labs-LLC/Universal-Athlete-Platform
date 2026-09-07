import { z } from 'zod';

export const organizationMembershipRoleSchema = z.enum([
  'ATHLETE',
  'COACH',
  'HEAD_COACH',
  'TEAM_ADMIN',
  'ORG_ADMIN',
  'ORG_OWNER',
]);

export type OrganizationMembershipRole = z.infer<typeof organizationMembershipRoleSchema>;

export const invitationStatusSchema = z.enum([
  'PENDING',
  'ACCEPTED',
  'DECLINED',
  'REVOKED',
  'EXPIRED',
]);

export type InvitationStatus = z.infer<typeof invitationStatusSchema>;

export const organizationMembershipStatusSchema = z.enum(['ACTIVE', 'REMOVED', 'LEFT']);

/** Pending invitation for the authenticated account — never includes rawToken. */
export const myInvitationSchema = z.object({
  id: z.string().min(1),
  organizationId: z.string().min(1),
  organizationName: z.string().min(1),
  teamId: z.string().nullable().optional(),
  teamName: z.string().nullable().optional(),
  role: organizationMembershipRoleSchema,
  expiresAt: z.string().min(1),
});

export type MyInvitation = z.infer<typeof myInvitationSchema>;

export const myInvitationListSchema = z.array(myInvitationSchema);

export const organizationSchema = z.object({
  id: z.string().min(1),
  name: z.string().min(1),
  status: z.string().min(1),
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
  version: z.number().int(),
});

export type Organization = z.infer<typeof organizationSchema>;

export const organizationListSchema = z.array(organizationSchema);

export const teamSchema = z.object({
  id: z.string().min(1),
  organizationId: z.string().min(1),
  name: z.string().min(1),
  status: z.string().min(1),
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
  version: z.number().int(),
});

export type Team = z.infer<typeof teamSchema>;

export const teamListSchema = z.array(teamSchema);

/** ACTIVE athlete team membership for consent / sharing team picker. */
export const myAthleteTeamSchema = z.object({
  membershipId: z.string().min(1),
  teamId: z.string().min(1),
  teamName: z.string().min(1),
  organizationId: z.string().min(1),
  organizationName: z.string().min(1),
  athleteId: z.string().min(1),
});

export type MyAthleteTeam = z.infer<typeof myAthleteTeamSchema>;

export const myAthleteTeamListSchema = z.array(myAthleteTeamSchema);

export const organizationMembershipSchema = z.object({
  id: z.string().min(1),
  organizationId: z.string().min(1),
  accountId: z.string().min(1),
  athleteId: z.string().nullable().optional(),
  role: organizationMembershipRoleSchema,
  status: organizationMembershipStatusSchema,
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
  version: z.number().int(),
});

export const teamMembershipSchema = z.object({
  id: z.string().min(1),
  teamId: z.string().min(1),
  accountId: z.string().min(1),
  athleteId: z.string().nullable().optional(),
  role: organizationMembershipRoleSchema,
  status: organizationMembershipStatusSchema,
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
  version: z.number().int(),
});

export const acceptInvitationResponseSchema = z.object({
  organizationMembership: organizationMembershipSchema.nullable().optional(),
  teamMembership: teamMembershipSchema.nullable().optional(),
});

export type AcceptInvitationResponse = z.infer<typeof acceptInvitationResponseSchema>;

/** Create response may include rawToken once; list endpoints omit it (null). */
export const invitationSchema = z.object({
  id: z.string().min(1),
  organizationId: z.string().min(1),
  teamId: z.string().nullable().optional(),
  invitedEmail: z.string().min(1),
  invitedAccountId: z.string().nullable().optional(),
  role: organizationMembershipRoleSchema,
  status: invitationStatusSchema,
  expiresAt: z.string().min(1),
  acceptedMembershipId: z.string().nullable().optional(),
  createdByAccountId: z.string().min(1),
  createdAt: z.string().min(1),
  updatedAt: z.string().min(1),
  version: z.number().int(),
  rawToken: z.string().nullable().optional(),
});

export type Invitation = z.infer<typeof invitationSchema>;

export const createInvitationRequestSchema = z.object({
  email: z.string().email().max(320),
  role: organizationMembershipRoleSchema,
});

export type CreateInvitationRequest = z.infer<typeof createInvitationRequestSchema>;

/** Roles allowed when creating an organization-scoped invitation. */
export const ORG_INVITE_ROLES = ['ORG_ADMIN'] as const satisfies readonly OrganizationMembershipRole[];

/** Roles allowed when creating a team-scoped invitation. */
export const TEAM_INVITE_ROLES = [
  'ATHLETE',
  'COACH',
  'HEAD_COACH',
  'TEAM_ADMIN',
] as const satisfies readonly OrganizationMembershipRole[];
