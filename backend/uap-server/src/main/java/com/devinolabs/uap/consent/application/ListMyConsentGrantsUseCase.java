package com.devinolabs.uap.consent.application;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.consent.domain.ConsentGrant;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort.TeamLifecycleRef;

@Service
public class ListMyConsentGrantsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final OrganizationMembershipPort membershipPort;
	private final ConsentGrantRepository consentGrantRepository;

	public ListMyConsentGrantsUseCase(
			AthleteContextPort athleteContextPort,
			OrganizationMembershipPort membershipPort,
			ConsentGrantRepository consentGrantRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.membershipPort = Objects.requireNonNull(membershipPort);
		this.consentGrantRepository = Objects.requireNonNull(consentGrantRepository);
	}

	@Transactional(readOnly = true)
	public List<ConsentGrantResult> execute(UUID accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		AthleteRef athlete;
		try {
			athlete = athleteContextPort.requireAthlete(accountId);
		}
		catch (AthleteNotFoundException ex) {
			throw new ConsentNotFoundException();
		}

		return consentGrantRepository.findAllByAthleteId(athlete.athleteId()).stream()
				.sorted(Comparator.comparing(ConsentGrant::createdAt).reversed())
				.map(this::toResult)
				.toList();
	}

	private ConsentGrantResult toResult(ConsentGrant grant) {
		TeamLifecycleRef lifecycle = membershipPort.findTeamLifecycle(grant.teamId()).orElse(null);
		if (lifecycle == null) {
			return ConsentGrantResult.from(grant);
		}
		return ConsentGrantResult.from(grant, lifecycle.teamName(), lifecycle.organizationName());
	}

}
