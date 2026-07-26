package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface WorkoutDayJpaRepository extends JpaRepository<WorkoutDayJpaEntity, UUID> {

	Optional<WorkoutDayJpaEntity> findByIdAndTrainingPlanIdAndAthleteId(
			UUID id,
			UUID trainingPlanId,
			UUID athleteId);

	List<WorkoutDayJpaEntity> findAllByTrainingPlanIdAndAthleteIdOrderByDisplayOrderAscCreatedAtAscIdAsc(
			UUID trainingPlanId,
			UUID athleteId);

	boolean existsByTrainingPlanIdAndNormalizedTitle(UUID trainingPlanId, String normalizedTitle);

	boolean existsByTrainingPlanIdAndNormalizedTitleAndIdNot(
			UUID trainingPlanId,
			String normalizedTitle,
			UUID id);

	boolean existsByTrainingPlanIdAndDisplayOrder(UUID trainingPlanId, int displayOrder);

	@Query("""
			select coalesce(max(d.displayOrder), -1)
			from WorkoutDayJpaEntity d
			where d.trainingPlanId = :trainingPlanId
			and d.athleteId = :athleteId
			""")
	int findMaxDisplayOrder(@Param("trainingPlanId") UUID trainingPlanId, @Param("athleteId") UUID athleteId);

	List<WorkoutDayJpaEntity> findAllByTrainingPlanIdAndDisplayOrderGreaterThanEqualOrderByDisplayOrderDesc(
			UUID trainingPlanId,
			int displayOrder);

	@Query("""
			select d from WorkoutDayJpaEntity d
			where d.trainingPlanId = :trainingPlanId
			and d.athleteId = :athleteId
			order by d.planWeekNumber asc, d.scheduledDayOfWeek asc, d.plannedStartTime asc,
				d.displayOrder asc, d.createdAt asc, d.id asc
			""")
	List<WorkoutDayJpaEntity> findAllOrderedByPlacement(
			@Param("trainingPlanId") UUID trainingPlanId,
			@Param("athleteId") UUID athleteId);

	List<WorkoutDayJpaEntity> findAllByIdInAndAthleteId(Collection<UUID> ids, UUID athleteId);

	@Query("""
			select count(d) > 0 from WorkoutDayJpaEntity d
			where d.trainingPlanId = :trainingPlanId
			and d.planWeekNumber = :planWeekNumber
			and d.scheduledDayOfWeek = :scheduledDayOfWeek
			and ((:plannedStartTime is null and d.plannedStartTime is null)
				or d.plannedStartTime = :plannedStartTime)
			and (:excludingId is null or d.id <> :excludingId)
			""")
	boolean existsDuplicatePlacement(
			@Param("trainingPlanId") UUID trainingPlanId,
			@Param("planWeekNumber") Integer planWeekNumber,
			@Param("scheduledDayOfWeek") DayOfWeek scheduledDayOfWeek,
			@Param("plannedStartTime") LocalTime plannedStartTime,
			@Param("excludingId") UUID excludingId);

}
