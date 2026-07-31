package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

interface WorkoutSessionEffortJpaRepository extends JpaRepository<WorkoutSessionEffortJpaEntity, UUID> {

	Optional<WorkoutSessionEffortJpaEntity> findByWorkoutOccurrenceIdAndAthleteId(UUID occurrenceId, UUID athleteId);

	boolean existsByWorkoutOccurrenceId(UUID occurrenceId);

}
