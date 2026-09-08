package com.devinolabs.uap.consent.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.consent.domain.ConsentGrant;
import com.devinolabs.uap.consent.domain.ConsentGrantId;
import com.devinolabs.uap.consent.domain.ConsentGrantStatus;

public interface ConsentGrantRepository {

	ConsentGrant save(ConsentGrant grant);

	Optional<ConsentGrant> findById(ConsentGrantId id);

	List<ConsentGrant> findActiveByAthleteIdAndTeamId(UUID athleteId, UUID teamId);

	List<ConsentGrant> findActiveByTeamId(UUID teamId);

	Optional<ConsentGrant> findActiveByTeamMembershipId(UUID teamMembershipId);

	List<ConsentGrant> findAllByAthleteId(UUID athleteId);

	List<ConsentGrant> findAllByAthleteIdAndStatus(UUID athleteId, ConsentGrantStatus status);

}
