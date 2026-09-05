package com.devinolabs.uap.organization.application;

import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;

public interface OrganizationAuditPort {

	void logCreated(OrganizationId organizationId, AccountId creatorAccountId);

	void logArchived(OrganizationId organizationId, AccountId actorAccountId);

	void logTeamCreated(UUID teamId, OrganizationId organizationId, AccountId actorAccountId);

	void logTeamArchived(UUID teamId, OrganizationId organizationId, AccountId actorAccountId);

}
