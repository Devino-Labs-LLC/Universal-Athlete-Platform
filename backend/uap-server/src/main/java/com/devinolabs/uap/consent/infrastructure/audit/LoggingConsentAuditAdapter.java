package com.devinolabs.uap.consent.infrastructure.audit;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.devinolabs.uap.consent.application.ConsentAuditPort;
import com.devinolabs.uap.consent.domain.ConsentGrantId;
import com.devinolabs.uap.consent.domain.ConsentScope;

@Component
class LoggingConsentAuditAdapter implements ConsentAuditPort {

	private static final Logger log = LoggerFactory.getLogger(LoggingConsentAuditAdapter.class);

	@Override
	public void consentGranted(
			ConsentGrantId consentGrantId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes) {
		log.info(
				"consent_audit event=CONSENT_GRANTED consentGrantId={} athleteId={} teamId={} organizationId={} teamMembershipId={} scopes={}",
				require(consentGrantId).value(),
				Objects.requireNonNull(athleteId),
				Objects.requireNonNull(teamId),
				Objects.requireNonNull(organizationId),
				Objects.requireNonNull(teamMembershipId),
				scopeNames(scopes));
	}

	@Override
	public void consentRevoked(
			ConsentGrantId consentGrantId,
			UUID athleteId,
			UUID teamId,
			UUID organizationId,
			UUID teamMembershipId,
			Set<ConsentScope> scopes) {
		log.info(
				"consent_audit event=CONSENT_REVOKED consentGrantId={} athleteId={} teamId={} organizationId={} teamMembershipId={} scopes={}",
				require(consentGrantId).value(),
				Objects.requireNonNull(athleteId),
				Objects.requireNonNull(teamId),
				Objects.requireNonNull(organizationId),
				Objects.requireNonNull(teamMembershipId),
				scopeNames(scopes));
	}

	private static ConsentGrantId require(ConsentGrantId consentGrantId) {
		return Objects.requireNonNull(consentGrantId);
	}

	private static String scopeNames(Set<ConsentScope> scopes) {
		Objects.requireNonNull(scopes);
		return scopes.stream().map(ConsentScope::name).sorted().collect(Collectors.joining(","));
	}

}
