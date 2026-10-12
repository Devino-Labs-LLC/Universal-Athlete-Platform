package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface WorkoutSessionEffortJpaRepository extends JpaRepository<WorkoutSessionEffortJpaEntity, UUID> {

	Optional<WorkoutSessionEffortJpaEntity> findByWorkoutOccurrenceIdAndAthleteId(UUID occurrenceId, UUID athleteId);

	boolean existsByWorkoutOccurrenceId(UUID occurrenceId);

	List<WorkoutSessionEffortJpaEntity> findByAthleteIdOrderBySubmittedAtDesc(UUID athleteId, Pageable pageable);

}
