package com.devinolabs.uap.athlete.infrastructure.persistence;

import com.devinolabs.uap.athlete.domain.AthleteGoal;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.GoalTarget;

final class AthleteGoalPersistenceMapper {

	private AthleteGoalPersistenceMapper() {
	}

	static AthleteGoalJpaEntity toEntity(AthleteGoal goal, boolean isNew) {
		GoalTarget target = goal.target();
		return new AthleteGoalJpaEntity(
				goal.id().value(),
				goal.athleteId().value(),
				goal.goalType(),
				goal.customGoalName(),
				goal.title(),
				goal.normalizedTitle(),
				goal.description(),
				goal.priority(),
				goal.status(),
				target == null ? null : target.value(),
				target == null ? null : target.unit(),
				target == null ? null : target.customUnit(),
				goal.targetDate(),
				goal.athleteSportId() == null ? null : goal.athleteSportId().value(),
				goal.createdAt(),
				goal.updatedAt(),
				goal.completedAt(),
				goal.version(),
				isNew);
	}

	static AthleteGoal toDomain(AthleteGoalJpaEntity entity) {
		GoalTarget target = GoalTarget.optional(
				entity.getTargetValue(),
				entity.getTargetUnit(),
				entity.getCustomTargetUnit());
		return AthleteGoal.rehydrate(
				AthleteGoalId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getGoalType(),
				entity.getCustomGoalName(),
				entity.getTitle(),
				entity.getNormalizedTitle(),
				entity.getDescription(),
				entity.getPriority(),
				entity.getStatus(),
				target,
				entity.getTargetDate(),
				entity.getAthleteSportId() == null ? null : AthleteSportId.of(entity.getAthleteSportId()),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getCompletedAt(),
				entity.getVersion());
	}

}
