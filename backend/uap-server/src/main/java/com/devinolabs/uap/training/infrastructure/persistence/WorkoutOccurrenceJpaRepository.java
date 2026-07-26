package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

interface WorkoutOccurrenceJpaRepository extends JpaRepository<WorkoutOccurrenceJpaEntity, UUID> {

	Optional<WorkoutOccurrenceJpaEntity> findByIdAndWorkoutDayIdAndAthleteId(
			UUID id,
			UUID workoutDayId,
			UUID athleteId);

	boolean existsByWorkoutDayIdAndAthleteIdAndScheduledDateAndStatusNot(
			UUID workoutDayId,
			UUID athleteId,
			LocalDate scheduledDate,
			WorkoutOccurrenceStatus status);

	@Query("""
			select o from WorkoutOccurrenceJpaEntity o
			where o.workoutDayId = :workoutDayId
			and o.athleteId = :athleteId
			and (:status is null or o.status = :status)
			and (:scheduledFrom is null or o.scheduledDate >= :scheduledFrom)
			and (:scheduledTo is null or o.scheduledDate <= :scheduledTo)
			order by o.scheduledDate desc, o.createdAt desc
			""")
	List<WorkoutOccurrenceJpaEntity> findFiltered(
			@Param("workoutDayId") UUID workoutDayId,
			@Param("athleteId") UUID athleteId,
			@Param("status") WorkoutOccurrenceStatus status,
			@Param("scheduledFrom") LocalDate scheduledFrom,
			@Param("scheduledTo") LocalDate scheduledTo);

}
