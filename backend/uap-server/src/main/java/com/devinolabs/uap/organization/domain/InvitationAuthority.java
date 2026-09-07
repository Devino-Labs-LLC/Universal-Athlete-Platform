package com.devinolabs.uap.organization.domain;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Server-side invitation and membership authority matrix (Slice B).
 */
public final class InvitationAuthority {

	private static final Set<OrganizationMembershipRole> TEAM_ROLES = EnumSet.of(
			OrganizationMembershipRole.ATHLETE,
			OrganizationMembershipRole.COACH,
			OrganizationMembershipRole.HEAD_COACH,
			OrganizationMembershipRole.TEAM_ADMIN);

	private InvitationAuthority() {
	}

	public static boolean canInviteTeamRole(
			OrganizationMembershipRole actorRole,
			boolean actorIsOrgScoped,
			OrganizationMembershipRole invitedRole) {
		Objects.requireNonNull(actorRole, "actorRole must not be null");
		Objects.requireNonNull(invitedRole, "invitedRole must not be null");
		if (!TEAM_ROLES.contains(invitedRole)) {
			return false;
		}
		if (actorIsOrgScoped) {
			return actorRole == OrganizationMembershipRole.ORG_ADMIN
					|| actorRole == OrganizationMembershipRole.ORG_OWNER;
		}
		return switch (actorRole) {
			case COACH -> invitedRole == OrganizationMembershipRole.ATHLETE;
			case HEAD_COACH -> invitedRole == OrganizationMembershipRole.ATHLETE
					|| invitedRole == OrganizationMembershipRole.COACH;
			case TEAM_ADMIN -> TEAM_ROLES.contains(invitedRole);
			default -> false;
		};
	}

	public static boolean canCreateOrgAdminInvitation(OrganizationMembershipRole actorOrgRole) {
		Objects.requireNonNull(actorOrgRole, "actorOrgRole must not be null");
		return actorOrgRole == OrganizationMembershipRole.ORG_ADMIN
				|| actorOrgRole == OrganizationMembershipRole.ORG_OWNER;
	}

	public static boolean canManageTeamRoster(
			OrganizationMembershipRole actorRole,
			boolean actorIsOrgScoped) {
		Objects.requireNonNull(actorRole, "actorRole must not be null");
		if (actorIsOrgScoped) {
			return actorRole == OrganizationMembershipRole.ORG_ADMIN
					|| actorRole == OrganizationMembershipRole.ORG_OWNER;
		}
		return actorRole == OrganizationMembershipRole.HEAD_COACH
				|| actorRole == OrganizationMembershipRole.TEAM_ADMIN;
	}

	public static boolean canRemoveTeamMember(
			OrganizationMembershipRole actorRole,
			boolean actorIsOrgScoped,
			OrganizationMembershipRole targetRole) {
		Objects.requireNonNull(actorRole, "actorRole must not be null");
		Objects.requireNonNull(targetRole, "targetRole must not be null");
		if (actorIsOrgScoped) {
			return (actorRole == OrganizationMembershipRole.ORG_ADMIN
					|| actorRole == OrganizationMembershipRole.ORG_OWNER)
					&& TEAM_ROLES.contains(targetRole);
		}
		return switch (actorRole) {
			case HEAD_COACH -> targetRole == OrganizationMembershipRole.ATHLETE
					|| targetRole == OrganizationMembershipRole.COACH;
			case TEAM_ADMIN -> TEAM_ROLES.contains(targetRole);
			default -> false;
		};
	}

	public static boolean canRemoveOrganizationMember(
			OrganizationMembershipRole actorRole,
			OrganizationMembershipRole targetRole) {
		Objects.requireNonNull(actorRole, "actorRole must not be null");
		Objects.requireNonNull(targetRole, "targetRole must not be null");
		if (targetRole == OrganizationMembershipRole.ORG_OWNER) {
			return false;
		}
		if (targetRole != OrganizationMembershipRole.ORG_ADMIN) {
			return false;
		}
		return actorRole == OrganizationMembershipRole.ORG_ADMIN
				|| actorRole == OrganizationMembershipRole.ORG_OWNER;
	}

	public static boolean canListTeamInvitations(
			OrganizationMembershipRole actorRole,
			boolean actorIsOrgScoped) {
		return canInviteTeamRole(actorRole, actorIsOrgScoped, OrganizationMembershipRole.ATHLETE)
				|| (actorIsOrgScoped && canCreateOrgAdminInvitation(actorRole))
				|| (!actorIsOrgScoped && (actorRole == OrganizationMembershipRole.HEAD_COACH
						|| actorRole == OrganizationMembershipRole.TEAM_ADMIN
						|| actorRole == OrganizationMembershipRole.COACH));
	}

}
