package com.devinolabs.uap.consent.application;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.consent.domain.ConsentGrant;
import com.devinolabs.uap.consent.domain.ConsentGrantId;

@Service
public class RevokeConsentGrantUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ConsentGrantRepository consentGrantRepository;
	private final ConsentAuditPort auditPort;
	private final Clock clock;

	public RevokeConsentGrantUseCase(
			AthleteContextPort athleteContextPort,
			ConsentGrantRepository consentGrantRepository,
			ConsentAuditPort auditPort,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.consentGrantRepository = Objects.requireNonNull(consentGrantRepository);
		this.auditPort = Objects.requireNonNull(auditPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public ConsentGrantResult execute(UUID accountId, UUID consentId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Objects.requireNonNull(consentId, "consentId must not be null");

		AthleteRef athlete;
		try {
			athlete = athleteContextPort.requireAthlete(accountId);
		}
		catch (AthleteNotFoundException ex) {
			throw new ConsentNotFoundException();
		}

		ConsentGrant grant = consentGrantRepository.findById(ConsentGrantId.of(consentId))
				.orElseThrow(ConsentNotFoundException::new);
		if (!grant.athleteId().equals(athlete.athleteId())) {
			throw new ConsentNotFoundException();
		}

		boolean changed = grant.revoke(clock);
		ConsentGrant saved = changed ? consentGrantRepository.save(grant) : grant;
		if (changed) {
			auditPort.consentRevoked(
					saved.id(),
					saved.athleteId(),
					saved.teamId(),
					saved.organizationId(),
					saved.teamMembershipId(),
					saved.scopes());
		}
		return ConsentGrantResult.from(saved);
	}

}
