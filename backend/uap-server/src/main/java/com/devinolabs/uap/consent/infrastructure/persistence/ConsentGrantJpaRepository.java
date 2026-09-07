package com.devinolabs.uap.consent.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.consent.domain.ConsentGrantStatus;

interface ConsentGrantJpaRepository extends JpaRepository<ConsentGrantJpaEntity, UUID> {

	List<ConsentGrantJpaEntity> findAllByAthleteIdAndTeamIdAndStatus(
			UUID athleteId,
			UUID teamId,
			ConsentGrantStatus status);

	Optional<ConsentGrantJpaEntity> findByTeamMembershipIdAndStatus(
			UUID teamMembershipId,
			ConsentGrantStatus status);

	List<ConsentGrantJpaEntity> findAllByAthleteIdOrderByCreatedAtDesc(UUID athleteId);

	List<ConsentGrantJpaEntity> findAllByAthleteIdAndStatus(UUID athleteId, ConsentGrantStatus status);

}
