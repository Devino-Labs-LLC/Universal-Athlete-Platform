package com.devinolabs.uap.consent.infrastructure.audit;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.audit.api.SecurityAuditRecord;
import com.devinolabs.uap.audit.api.SecurityAuditWriter;
import com.devinolabs.uap.consent.application.ConsentAuditPort;
import com.devinolabs.uap.consent.domain.ConsentGrantId;
import com.devinolabs.uap.consent.domain.ConsentScope;

@Component
class DurableConsentAuditAdapter implements ConsentAuditPort {

	private final SecurityAuditWriter auditWriter;

	DurableConsentAuditAdapter(SecurityAuditWriter auditWriter) {
		this.auditWriter = Objects.requireNonNull(auditWriter);
	}

	@Override
	public void consentGranted(
			ConsentGrantId consentGrantId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes) {
		append("CONSENT_GRANTED", consentGrantId, actorAccountId, athleteId, teamId, organizationId, teamMembershipId, scopes);
	}

	@Override
	public void consentRevoked(
			ConsentGrantId consentGrantId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes) {
		append("CONSENT_REVOKED", consentGrantId, actorAccountId, athleteId, teamId, organizationId, teamMembershipId, scopes);
	}

	private void append(
			String eventType,
			ConsentGrantId consentGrantId,
			UUID actorAccountId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes) {
		Objects.requireNonNull(consentGrantId);
		Objects.requireNonNull(actorAccountId);
		Objects.requireNonNull(athleteId);
		Objects.requireNonNull(teamId);
		Objects.requireNonNull(organizationId);
		Objects.requireNonNull(teamMembershipId);
		Objects.requireNonNull(scopes);
		String metadata = "{\"scopes\":["
				+ scopes.stream().map(ConsentScope::name).sorted().map(name -> "\"" + name + "\"").collect(Collectors.joining(","))
				+ "],\"membershipId\":\""
				+ teamMembershipId
				+ "\"}";
		auditWriter.append(SecurityAuditRecord.of(
				eventType,
				actorAccountId,
				null,
				athleteId,
				organizationId,
				teamId,
				"CONSENT_GRANT",
				consentGrantId.value(),
				metadata));
	}

}
