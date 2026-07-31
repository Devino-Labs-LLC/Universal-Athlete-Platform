package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkoutSessionEffortRevisionJpaRepository extends JpaRepository<WorkoutSessionEffortRevisionJpaEntity, UUID> {

	@Query("""
			select r from WorkoutSessionEffortRevisionJpaEntity r
			where r.workoutSessionEffortId = :effortId
			and r.athleteId = :athleteId
			order by r.revisionNumber asc
			""")
	List<WorkoutSessionEffortRevisionJpaEntity> findAllByEffortIdAndAthleteIdOrderByRevisionNumber(
			@Param("effortId") UUID effortId,
			@Param("athleteId") UUID athleteId);

	int countByWorkoutSessionEffortId(UUID effortId);

}
