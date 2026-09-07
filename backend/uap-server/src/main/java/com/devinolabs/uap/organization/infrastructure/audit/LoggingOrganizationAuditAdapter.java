package com.devinolabs.uap.organization.infrastructure.audit;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devinolabs.uap.organization.application.OrganizationAuditPort;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipRole;
import com.devinolabs.uap.organization.domain.TeamId;

@Component
class LoggingOrganizationAuditAdapter implements OrganizationAuditPort {

	private static final Logger log = LoggerFactory.getLogger(LoggingOrganizationAuditAdapter.class);

	@Override
	public void logCreated(OrganizationId organizationId, AccountId creatorAccountId) {
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(creatorAccountId);
		log.info("organization_audit event=ORGANIZATION_CREATED organizationId={} accountId={}",
				organizationId.value(), creatorAccountId.value());
	}

	@Override
	public void logArchived(OrganizationId organizationId, AccountId actorAccountId) {
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(actorAccountId);
		log.info("organization_audit event=ORGANIZATION_ARCHIVED organizationId={} accountId={}",
				organizationId.value(), actorAccountId.value());
	}

	@Override
	public void logTeamCreated(UUID teamId, OrganizationId organizationId, AccountId actorAccountId) {
		Objects.requireNonNull(teamId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(actorAccountId);
		log.info("organization_audit event=TEAM_CREATED teamId={} organizationId={} accountId={}",
				teamId, organizationId.value(), actorAccountId.value());
	}

	@Override
	public void logTeamArchived(UUID teamId, OrganizationId organizationId, AccountId actorAccountId) {
		Objects.requireNonNull(teamId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(actorAccountId);
		log.info("organization_audit event=TEAM_ARCHIVED teamId={} organizationId={} accountId={}",
				teamId, organizationId.value(), actorAccountId.value());
	}

	@Override
	public void invitationCreated(
			InvitationId invitationId,
			OrganizationId organizationId,
			TeamId teamId,
			OrganizationMembershipRole role,
			AccountId actorAccountId) {
		Objects.requireNonNull(invitationId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(role);
		Objects.requireNonNull(actorAccountId);
		log.info(
				"organization_audit event=INVITATION_CREATED invitationId={} organizationId={} teamId={} role={} accountId={}",
				invitationId.value(),
				organizationId.value(),
				teamId == null ? null : teamId.value(),
				role,
				actorAccountId.value());
	}

	@Override
	public void invitationRevoked(InvitationId invitationId, OrganizationId organizationId, AccountId actorAccountId) {
		Objects.requireNonNull(invitationId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(actorAccountId);
		log.info("organization_audit event=INVITATION_REVOKED invitationId={} organizationId={} accountId={}",
				invitationId.value(), organizationId.value(), actorAccountId.value());
	}

	@Override
	public void invitationAccepted(InvitationId invitationId, OrganizationId organizationId, AccountId actorAccountId) {
		Objects.requireNonNull(invitationId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(actorAccountId);
		log.info("organization_audit event=INVITATION_ACCEPTED invitationId={} organizationId={} accountId={}",
				invitationId.value(), organizationId.value(), actorAccountId.value());
	}

	@Override
	public void invitationDeclined(InvitationId invitationId, OrganizationId organizationId, AccountId actorAccountId) {
		Objects.requireNonNull(invitationId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(actorAccountId);
		log.info("organization_audit event=INVITATION_DECLINED invitationId={} organizationId={} accountId={}",
				invitationId.value(), organizationId.value(), actorAccountId.value());
	}

	@Override
	public void membershipActivated(
			UUID membershipId,
			OrganizationId organizationId,
			TeamId teamId,
			OrganizationMembershipRole role,
			AccountId accountId) {
		Objects.requireNonNull(membershipId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(role);
		Objects.requireNonNull(accountId);
		log.info(
				"organization_audit event=MEMBERSHIP_ACTIVATED membershipId={} organizationId={} teamId={} role={} accountId={}",
				membershipId,
				organizationId.value(),
				teamId == null ? null : teamId.value(),
				role,
				accountId.value());
	}

	@Override
	public void membershipRemoved(
			UUID membershipId,
			OrganizationId organizationId,
			TeamId teamId,
			AccountId actorAccountId,
			AccountId subjectAccountId) {
		Objects.requireNonNull(membershipId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(actorAccountId);
		Objects.requireNonNull(subjectAccountId);
		log.info(
				"organization_audit event=MEMBERSHIP_REMOVED membershipId={} organizationId={} teamId={} actorAccountId={} subjectAccountId={}",
				membershipId,
				organizationId.value(),
				teamId == null ? null : teamId.value(),
				actorAccountId.value(),
				subjectAccountId.value());
	}

	@Override
	public void membershipLeft(
			UUID membershipId,
			OrganizationId organizationId,
			TeamId teamId,
			AccountId accountId) {
		Objects.requireNonNull(membershipId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(accountId);
		log.info(
				"organization_audit event=MEMBERSHIP_LEFT membershipId={} organizationId={} teamId={} accountId={}",
				membershipId,
				organizationId.value(),
				teamId == null ? null : teamId.value(),
				accountId.value());
	}

}
