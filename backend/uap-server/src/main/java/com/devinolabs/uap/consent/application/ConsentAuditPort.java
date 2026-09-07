package com.devinolabs.uap.consent.application;

import java.util.Set;
import java.util.UUID;

import com.devinolabs.uap.consent.domain.ConsentGrantId;
import com.devinolabs.uap.consent.domain.ConsentScope;

public interface ConsentAuditPort {

	void consentGranted(
			ConsentGrantId consentGrantId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes);

	void consentRevoked(
			ConsentGrantId consentGrantId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes);

}
