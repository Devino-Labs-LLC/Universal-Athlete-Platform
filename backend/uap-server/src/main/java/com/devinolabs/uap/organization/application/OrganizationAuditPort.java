package com.devinolabs.uap.organization.application;

import java.util.UUID;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.TeamId;

public interface OrganizationAuditPort {

	void logCreated(OrganizationId organizationId, AccountId creatorAccountId);

	void logArchived(OrganizationId organizationId, AccountId actorAccountId);

	void logTeamCreated(UUID teamId, OrganizationId organizationId, AccountId actorAccountId);

	void logTeamArchived(UUID teamId, OrganizationId organizationId, AccountId actorAccountId);

	void invitationCreated(
			InvitationId invitationId,
			OrganizationId organizationId,
			TeamId teamId,
			OrganizationMembershipRole role,
			AccountId actorAccountId);

	void invitationRevoked(InvitationId invitationId, OrganizationId organizationId, AccountId actorAccountId);

	void invitationAccepted(InvitationId invitationId, OrganizationId organizationId, AccountId actorAccountId);

	void invitationDeclined(InvitationId invitationId, OrganizationId organizationId, AccountId actorAccountId);

	void membershipActivated(
			UUID membershipId,
			OrganizationId organizationId,
			TeamId teamId,
			OrganizationMembershipRole role,
			AccountId accountId);

	void membershipRemoved(
			UUID membershipId,
			OrganizationId organizationId,
			TeamId teamId,
			AccountId actorAccountId,
			AccountId subjectAccountId);

	void membershipLeft(
			UUID membershipId,
			OrganizationId organizationId,
			TeamId teamId,
			AccountId accountId);

}
