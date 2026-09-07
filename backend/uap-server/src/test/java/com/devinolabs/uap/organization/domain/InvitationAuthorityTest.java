package com.devinolabs.uap.organization.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InvitationAuthorityTest {

	@Test
	void orgScopedActorsCanInviteTeamRolesAndManageRoster() {
		assertThat(InvitationAuthority.canInviteTeamRole(
				OrganizationMembershipRole.ORG_OWNER, true, OrganizationMembershipRole.ATHLETE)).isTrue();
		assertThat(InvitationAuthority.canInviteTeamRole(
				OrganizationMembershipRole.ORG_ADMIN, true, OrganizationMembershipRole.HEAD_COACH)).isTrue();
		assertThat(InvitationAuthority.canInviteTeamRole(
				OrganizationMembershipRole.ORG_OWNER, true, OrganizationMembershipRole.ORG_ADMIN)).isFalse();
		assertThat(InvitationAuthority.canManageTeamRoster(OrganizationMembershipRole.ORG_OWNER, true)).isTrue();
		assertThat(InvitationAuthority.canManageTeamRoster(OrganizationMembershipRole.ORG_ADMIN, true)).isTrue();
		assertThat(InvitationAuthority.canManageTeamRoster(OrganizationMembershipRole.ATHLETE, true)).isFalse();
		assertThat(InvitationAuthority.canCreateOrgAdminInvitation(OrganizationMembershipRole.ORG_OWNER)).isTrue();
		assertThat(InvitationAuthority.canCreateOrgAdminInvitation(OrganizationMembershipRole.ORG_ADMIN)).isTrue();
		assertThat(InvitationAuthority.canCreateOrgAdminInvitation(OrganizationMembershipRole.HEAD_COACH)).isFalse();
	}

	@Test
	void teamScopedInviteAndRemoveMatrixMatchesRoles() {
		assertThat(InvitationAuthority.canInviteTeamRole(
				OrganizationMembershipRole.COACH, false, OrganizationMembershipRole.ATHLETE)).isTrue();
		assertThat(InvitationAuthority.canInviteTeamRole(
				OrganizationMembershipRole.COACH, false, OrganizationMembershipRole.COACH)).isFalse();
		assertThat(InvitationAuthority.canInviteTeamRole(
				OrganizationMembershipRole.HEAD_COACH, false, OrganizationMembershipRole.COACH)).isTrue();
		assertThat(InvitationAuthority.canInviteTeamRole(
				OrganizationMembershipRole.HEAD_COACH, false, OrganizationMembershipRole.HEAD_COACH)).isFalse();
		assertThat(InvitationAuthority.canInviteTeamRole(
				OrganizationMembershipRole.TEAM_ADMIN, false, OrganizationMembershipRole.HEAD_COACH)).isTrue();
		assertThat(InvitationAuthority.canInviteTeamRole(
				OrganizationMembershipRole.ATHLETE, false, OrganizationMembershipRole.ATHLETE)).isFalse();

		assertThat(InvitationAuthority.canRemoveTeamMember(
				OrganizationMembershipRole.ORG_OWNER, true, OrganizationMembershipRole.ATHLETE)).isTrue();
		assertThat(InvitationAuthority.canRemoveTeamMember(
				OrganizationMembershipRole.HEAD_COACH, false, OrganizationMembershipRole.COACH)).isTrue();
		assertThat(InvitationAuthority.canRemoveTeamMember(
				OrganizationMembershipRole.HEAD_COACH, false, OrganizationMembershipRole.HEAD_COACH)).isFalse();
		assertThat(InvitationAuthority.canRemoveTeamMember(
				OrganizationMembershipRole.TEAM_ADMIN, false, OrganizationMembershipRole.HEAD_COACH)).isTrue();
		assertThat(InvitationAuthority.canRemoveTeamMember(
				OrganizationMembershipRole.COACH, false, OrganizationMembershipRole.ATHLETE)).isFalse();
		assertThat(InvitationAuthority.canManageTeamRoster(OrganizationMembershipRole.HEAD_COACH, false)).isTrue();
		assertThat(InvitationAuthority.canManageTeamRoster(OrganizationMembershipRole.TEAM_ADMIN, false)).isTrue();
		assertThat(InvitationAuthority.canManageTeamRoster(OrganizationMembershipRole.COACH, false)).isFalse();
	}

	@Test
	void organizationMemberRemovalAllowsOnlyAdminTargets() {
		assertThat(InvitationAuthority.canRemoveOrganizationMember(
				OrganizationMembershipRole.ORG_OWNER, OrganizationMembershipRole.ORG_ADMIN)).isTrue();
		assertThat(InvitationAuthority.canRemoveOrganizationMember(
				OrganizationMembershipRole.ORG_ADMIN, OrganizationMembershipRole.ORG_ADMIN)).isTrue();
		assertThat(InvitationAuthority.canRemoveOrganizationMember(
				OrganizationMembershipRole.ORG_OWNER, OrganizationMembershipRole.ORG_OWNER)).isFalse();
		assertThat(InvitationAuthority.canRemoveOrganizationMember(
				OrganizationMembershipRole.ORG_ADMIN, OrganizationMembershipRole.ATHLETE)).isFalse();
		assertThat(InvitationAuthority.canRemoveOrganizationMember(
				OrganizationMembershipRole.HEAD_COACH, OrganizationMembershipRole.ORG_ADMIN)).isFalse();
	}

	@Test
	void listTeamInvitationsCapabilityFollowsInviteOrRosterRoles() {
		assertThat(InvitationAuthority.canListTeamInvitations(OrganizationMembershipRole.ORG_OWNER, true)).isTrue();
		assertThat(InvitationAuthority.canListTeamInvitations(OrganizationMembershipRole.ORG_ADMIN, true)).isTrue();
		assertThat(InvitationAuthority.canListTeamInvitations(OrganizationMembershipRole.COACH, false)).isTrue();
		assertThat(InvitationAuthority.canListTeamInvitations(OrganizationMembershipRole.HEAD_COACH, false)).isTrue();
		assertThat(InvitationAuthority.canListTeamInvitations(OrganizationMembershipRole.TEAM_ADMIN, false)).isTrue();
		assertThat(InvitationAuthority.canListTeamInvitations(OrganizationMembershipRole.ATHLETE, false)).isFalse();
		assertThat(InvitationAuthority.canListTeamInvitations(OrganizationMembershipRole.ATHLETE, true)).isFalse();
	}

}
