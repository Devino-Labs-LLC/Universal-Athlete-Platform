package com.devinolabs.uap.consent.application;

import java.time.Clock;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.consent.domain.ConsentGrant;
import com.devinolabs.uap.consent.domain.ConsentGrantId;
import com.devinolabs.uap.consent.domain.ConsentScope;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamLifecycleRef;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamMembershipRef;

@Service
public class CreateConsentGrantUseCase {

	private static final String ACTIVE = "ACTIVE";

	private final AthleteContextPort athleteContextPort;
	private final OrganizationMembershipPort membershipPort;
	private final ConsentGrantRepository consentGrantRepository;
	private final ConsentAuditPort auditPort;
	private final Clock clock;

	public CreateConsentGrantUseCase(
			AthleteContextPort athleteContextPort,
			OrganizationMembershipPort membershipPort,
			ConsentGrantRepository consentGrantRepository,
			ConsentAuditPort auditPort,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.membershipPort = Objects.requireNonNull(membershipPort);
		this.consentGrantRepository = Objects.requireNonNull(consentGrantRepository);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public ConsentGrantResult execute(UUID accountId, UUID teamId, List<String> rawScopes) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(teamId, "teamId must not be null");
		Set<ConsentScope> scopes = parseScopes(rawScopes);

		AthleteRef athlete;
		try {
			athlete = athleteContextPort.requireAthlete(accountId);
		}
		catch (AthleteNotFoundException ex) {
			throw new ConsentNotFoundException();
		}

		TeamMembershipRef membership = membershipPort.findActiveAthleteTeamMembership(accountId, teamId)
				.orElseThrow(ConsentNotFoundException::new);
		if (!athlete.athleteId().equals(membership.athleteId())) {
			throw new ConsentNotFoundException();
		}

		TeamLifecycleRef lifecycle = membershipPort.findTeamLifecycle(teamId)
				.orElseThrow(ConsentNotFoundException::new);
		if (!ACTIVE.equals(lifecycle.teamStatus())) {
			throw new ConsentConflictException("TEAM_ARCHIVED", "Archived team cannot receive consent grants");
		}
		if (!ACTIVE.equals(lifecycle.organizationStatus())) {
			throw new ConsentConflictException(
					"ORGANIZATION_ARCHIVED",
					"Archived organization cannot receive consent grants");
		}

		if (consentGrantRepository.findActiveByTeamMembershipId(membership.membershipId()).isPresent()) {
			throw new ConsentConflictException(
					"ACTIVE_GRANT_EXISTS",
					"An ACTIVE consent grant already exists for this membership generation");
		}

		ConsentGrant grant = ConsentGrant.grant(
				ConsentGrantId.generate(),
				athlete.athleteId(),
				membership.teamId(),
				membership.organizationId(),
				membership.membershipId(),
				scopes,
				clock);

		ConsentGrant saved;
		try {
			saved = consentGrantRepository.save(grant);
		}
		catch (DataIntegrityViolationException ex) {
			throw new ConsentConflictException(
					"ACTIVE_GRANT_EXISTS",
					"An ACTIVE consent grant already exists for this membership generation");
		}

		auditPort.consentGranted(
				saved.id(),
				accountId,
				saved.athleteId(),
				saved.teamId(),
				saved.organizationId(),
				saved.teamMembershipId(),
				saved.scopes());
		return ConsentGrantResult.from(saved, lifecycle.teamName(), lifecycle.organizationName());
	}

	private static Set<ConsentScope> parseScopes(List<String> rawScopes) {
		if (rawScopes == null || rawScopes.isEmpty()) {
			throw new IllegalArgumentException("scopes must not be empty");
		}
		EnumSet<ConsentScope> scopes = EnumSet.noneOf(ConsentScope.class);
		for (String raw : rawScopes) {
			if (raw == null || raw.isBlank()) {
				throw new IllegalArgumentException("scopes must not contain blank values");
			}
			try {
				scopes.add(ConsentScope.valueOf(raw.trim().toUpperCase(Locale.ROOT)));
			}
			catch (IllegalArgumentException ex) {
				throw new IllegalArgumentException("Invalid consent scope: " + raw);
			}
		}
		if (scopes.isEmpty()) {
			throw new IllegalArgumentException("scopes must not be empty");
		}
		return scopes;
	}

}
