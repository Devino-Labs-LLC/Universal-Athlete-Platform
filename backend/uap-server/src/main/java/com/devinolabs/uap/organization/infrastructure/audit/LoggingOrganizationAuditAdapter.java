package com.devinolabs.uap.organization.infrastructure.audit;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devinolabs.uap.organization.application.OrganizationAuditPort;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;

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

}
