package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalType;

interface AthleteGoalJpaRepository extends JpaRepository<AthleteGoalJpaEntity, UUID> {

	Optional<AthleteGoalJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	@Query("""
			select g from AthleteGoalJpaEntity g
			where g.athleteId = :athleteId
			and (:status is null or g.status = :status)
			and (:goalType is null or g.goalType = :goalType)
			""")
	List<AthleteGoalJpaEntity> findFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("status") GoalStatus status,
			@Param("goalType") GoalType goalType);

	boolean existsByAthleteIdAndGoalTypeAndNormalizedTitleAndStatusInAndIdNot(
			UUID athleteId,
			GoalType goalType,
			String normalizedTitle,
			Collection<GoalStatus> statuses,
			UUID excludingId);

	boolean existsByAthleteIdAndGoalTypeAndNormalizedTitleAndStatusIn(
			UUID athleteId,
			GoalType goalType,
			String normalizedTitle,
			Collection<GoalStatus> statuses);

}
