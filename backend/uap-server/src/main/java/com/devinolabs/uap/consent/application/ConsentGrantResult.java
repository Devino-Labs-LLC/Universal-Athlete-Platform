package com.devinolabs.uap.consent.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.consent.domain.ConsentGrant;
import com.devinolabs.uap.consent.domain.ConsentGrantStatus;
import com.devinolabs.uap.consent.domain.ConsentScope;

public record ConsentGrantResult(
		UUID id,
		UUID athleteId,
		UUID teamId,
		UUID organizationId,
		UUID teamMembershipId,
		List<String> scopes,
		ConsentGrantStatus status,
		Instant createdAt,
		Instant revokedAt,
		Instant updatedAt,
		long version,
		String teamName,
		String organizationName) {

	public static ConsentGrantResult from(ConsentGrant grant) {
		return from(grant, null, null);
	}

	public static ConsentGrantResult from(ConsentGrant grant, String teamName, String organizationName) {
		Objects.requireNonNull(grant, "grant must not be null");
		return new ConsentGrantResult(
				grant.id().value(),
				grant.athleteId(),
				grant.teamId(),
				grant.organizationId(),
				grant.teamMembershipId(),
				grant.scopes().stream().map(ConsentScope::name).sorted().toList(),
				grant.status(),
				grant.createdAt(),
				grant.revokedAt(),
				grant.updatedAt(),
				grant.version(),
				teamName,
				organizationName);
	}

}
