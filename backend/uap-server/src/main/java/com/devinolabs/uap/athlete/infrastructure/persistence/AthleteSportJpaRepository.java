package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.devinolabs.uap.athlete.domain.SportType;

interface AthleteSportJpaRepository extends JpaRepository<AthleteSportJpaEntity, UUID> {

	List<AthleteSportJpaEntity> findByAthleteId(UUID athleteId);

	Optional<AthleteSportJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	Optional<AthleteSportJpaEntity> findByAthleteIdAndPrimarySportTrue(UUID athleteId);

	boolean existsByAthleteIdAndSportType(UUID athleteId, SportType sportType);

	boolean existsByAthleteIdAndCustomSportNameNormalized(UUID athleteId, String customSportNameNormalized);

}
