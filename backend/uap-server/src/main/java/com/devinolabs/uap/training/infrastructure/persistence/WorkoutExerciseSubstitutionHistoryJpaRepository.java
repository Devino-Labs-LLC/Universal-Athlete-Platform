package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkoutExerciseSubstitutionHistoryJpaRepository
		extends JpaRepository<WorkoutExerciseSubstitutionHistoryJpaEntity, UUID> {

	@Query("""
			select h from WorkoutExerciseSubstitutionHistoryJpaEntity h
			where h.workoutExerciseExecutionId = :executionId
			and h.athleteId = :athleteId
			order by h.changedAt asc, h.id asc
			""")
	List<WorkoutExerciseSubstitutionHistoryJpaEntity> findAllForExecution(
			@Param("executionId") UUID executionId,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select h from WorkoutExerciseSubstitutionHistoryJpaEntity h
			where h.workoutOccurrenceId = :occurrenceId
			and h.athleteId = :athleteId
			order by h.changedAt asc, h.id asc
			""")
	List<WorkoutExerciseSubstitutionHistoryJpaEntity> findAllForOccurrence(
			@Param("occurrenceId") UUID occurrenceId,
			@Param("athleteId") UUID athleteId);

}
