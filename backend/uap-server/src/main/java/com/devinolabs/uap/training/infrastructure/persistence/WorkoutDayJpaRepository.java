package com.devinolabs.uap.training.infrastructure.persistence;

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

}
