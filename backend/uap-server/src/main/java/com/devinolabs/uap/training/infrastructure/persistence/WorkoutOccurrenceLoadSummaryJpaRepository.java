package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.MovementPattern;

interface WorkoutOccurrenceLoadSummaryJpaRepository extends JpaRepository<WorkoutOccurrenceLoadSummaryJpaEntity, UUID> {

	@Query("""
			select distinct s from WorkoutOccurrenceLoadSummaryJpaEntity s
			left join fetch s.categories
			where s.workoutOccurrenceId = :occurrenceId
			and s.athleteId = :athleteId
			""")
	Optional<WorkoutOccurrenceLoadSummaryJpaEntity> findByOccurrenceIdAndAthleteIdWithCategories(
			@Param("occurrenceId") UUID occurrenceId,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select distinct s from WorkoutOccurrenceLoadSummaryJpaEntity s
			left join fetch s.movements
			where s.id = :id
			""")
	Optional<WorkoutOccurrenceLoadSummaryJpaEntity> findByIdWithMovements(@Param("id") UUID id);

	@Modifying
	@Query("delete from WorkoutOccurrenceLoadSummaryJpaEntity s where s.athleteId = :athleteId")
	void deleteAllByAthleteId(@Param("athleteId") UUID athleteId);

	@Query("""
			select distinct s from WorkoutOccurrenceLoadSummaryJpaEntity s
			left join s.categories c
			left join s.movements m
			where s.athleteId = :athleteId
			and s.scheduledDate >= :startDate
			and s.scheduledDate <= :endDate
			and (:trainingPlanId is null or s.trainingPlanId = :trainingPlanId)
			and (:workoutDayId is null or s.workoutDayId = :workoutDayId)
			and (:category is null or c.category = :category)
			and (:movementPattern is null or m.primaryMovementPattern = :movementPattern)
			order by s.scheduledDate desc, s.workoutOccurrenceId desc
			""")
	List<WorkoutOccurrenceLoadSummaryJpaEntity> findFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("trainingPlanId") UUID trainingPlanId,
			@Param("workoutDayId") UUID workoutDayId,
			@Param("category") ExerciseDefinitionCategory category,
			@Param("movementPattern") MovementPattern movementPattern);

	@Query("""
			select distinct s from WorkoutOccurrenceLoadSummaryJpaEntity s
			left join fetch s.categories
			where s.id in :ids
			""")
	List<WorkoutOccurrenceLoadSummaryJpaEntity> findByIdsWithCategories(@Param("ids") Collection<UUID> ids);

	@Query("""
			select distinct s from WorkoutOccurrenceLoadSummaryJpaEntity s
			left join fetch s.movements
			where s.id in :ids
			""")
	List<WorkoutOccurrenceLoadSummaryJpaEntity> findByIdsWithMovements(@Param("ids") Collection<UUID> ids);

	@Query("""
			select count(distinct s.id) from WorkoutOccurrenceLoadSummaryJpaEntity s
			left join s.categories c
			left join s.movements m
			where s.athleteId = :athleteId
			and s.scheduledDate >= :startDate
			and s.scheduledDate <= :endDate
			and (:trainingPlanId is null or s.trainingPlanId = :trainingPlanId)
			and (:workoutDayId is null or s.workoutDayId = :workoutDayId)
			and (:category is null or c.category = :category)
			and (:movementPattern is null or m.primaryMovementPattern = :movementPattern)
			""")
	long countFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("trainingPlanId") UUID trainingPlanId,
			@Param("workoutDayId") UUID workoutDayId,
			@Param("category") ExerciseDefinitionCategory category,
			@Param("movementPattern") MovementPattern movementPattern);

}
