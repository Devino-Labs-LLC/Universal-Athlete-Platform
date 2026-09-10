package com.devinolabs.uap.organization.infrastructure.audit;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.organization.application.OrganizationAuditPort;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.TeamId;

@Component
class DurableOrganizationAuditAdapter implements OrganizationAuditPort {

	private final SecurityAuditWriter auditWriter;

	DurableOrganizationAuditAdapter(SecurityAuditWriter auditWriter) {
		this.auditWriter = Objects.requireNonNull(auditWriter);
	}

	@Override
	public void logCreated(OrganizationId organizationId, AccountId creatorAccountId) {
		append(
				"ORGANIZATION_CREATED",
				creatorAccountId.value(),
				null,
				organizationId.value(),
				null,
				"ORGANIZATION",
				organizationId.value(),
				null);
	}

	@Override
	public void logArchived(OrganizationId organizationId, AccountId actorAccountId) {
		append(
				"ORGANIZATION_ARCHIVED",
				actorAccountId.value(),
				null,
				organizationId.value(),
				null,
				"ORGANIZATION",
				organizationId.value(),
				null);
	}

	@Override
	public void logTeamCreated(UUID teamId, OrganizationId organizationId, AccountId actorAccountId) {
		append(
				"TEAM_CREATED",
				actorAccountId.value(),
				null,
				organizationId.value(),
				teamId,
				"TEAM",
				teamId,
				null);
	}

	@Override
	public void logTeamArchived(UUID teamId, OrganizationId organizationId, AccountId actorAccountId) {
		append(
				"TEAM_ARCHIVED",
				actorAccountId.value(),
				null,
				organizationId.value(),
				teamId,
				"TEAM",
				teamId,
				null);
	}

	@Override
	public void invitationCreated(
			InvitationId invitationId,
			OrganizationId organizationId,
			TeamId teamId,
			OrganizationMembershipRole role,
			AccountId actorAccountId) {
		append(
				"INVITATION_CREATED",
				actorAccountId.value(),
				null,
				organizationId.value(),
				teamId == null ? null : teamId.value(),
				"INVITATION",
				invitationId.value(),
				"{\"role\":\"" + role.name() + "\"}");
	}

	@Override
	public void invitationRevoked(InvitationId invitationId, OrganizationId organizationId, AccountId actorAccountId) {
		append(
				"INVITATION_REVOKED",
				actorAccountId.value(),
				null,
				organizationId.value(),
				null,
				"INVITATION",
				invitationId.value(),
				null);
	}

	@Override
	public void invitationAccepted(InvitationId invitationId, OrganizationId organizationId, AccountId actorAccountId) {
		append(
				"INVITATION_ACCEPTED",
				actorAccountId.value(),
				actorAccountId.value(),
				organizationId.value(),
				null,
				"INVITATION",
				invitationId.value(),
				null);
	}

	@Override
	public void invitationDeclined(InvitationId invitationId, OrganizationId organizationId, AccountId actorAccountId) {
		append(
				"INVITATION_DECLINED",
				actorAccountId.value(),
				actorAccountId.value(),
				organizationId.value(),
				null,
				"INVITATION",
				invitationId.value(),
				null);
	}

	@Override
	public void membershipActivated(
			UUID membershipId,
			OrganizationId organizationId,
			TeamId teamId,
			OrganizationMembershipRole role,
			AccountId accountId) {
		append(
				"MEMBERSHIP_ACTIVATED",
				accountId.value(),
				accountId.value(),
				organizationId.value(),
				teamId == null ? null : teamId.value(),
				"MEMBERSHIP",
				membershipId,
				"{\"role\":\"" + role.name() + "\"}");
	}

	@Override
	public void membershipRemoved(
			UUID membershipId,
			OrganizationId organizationId,
			TeamId teamId,
			AccountId actorAccountId,
			AccountId subjectAccountId) {
		append(
				"MEMBERSHIP_REMOVED",
				actorAccountId.value(),
				subjectAccountId.value(),
				organizationId.value(),
				teamId == null ? null : teamId.value(),
				"MEMBERSHIP",
				membershipId,
				null);
	}

	@Override
	public void membershipLeft(
			UUID membershipId,
			OrganizationId organizationId,
			TeamId teamId,
			AccountId accountId) {
		append(
				"MEMBERSHIP_LEFT",
				accountId.value(),
				accountId.value(),
				organizationId.value(),
				teamId == null ? null : teamId.value(),
				"MEMBERSHIP",
				membershipId,
				null);
	}

	private void append(
			String eventType,
			UUID actorAccountId,
			UUID subjectAccountId,
			UUID organizationId,
			UUID teamId,
			String resourceType,
			UUID resourceId,
			String metadataJson) {
		Objects.requireNonNull(eventType);
		Objects.requireNonNull(actorAccountId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(resourceType);
		Objects.requireNonNull(resourceId);
		auditWriter.append(SecurityAuditRecord.of(
				eventType,
				actorAccountId,
				subjectAccountId,
				null,
				organizationId,
				teamId,
				resourceType,
				resourceId,
				metadataJson));
	}

}
