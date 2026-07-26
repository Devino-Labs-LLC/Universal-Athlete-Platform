package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteGoalId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.AthleteSportId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;

final class TrainingPlanPersistenceMapper {

	private TrainingPlanPersistenceMapper() {
	}

	static TrainingPlanJpaEntity toEntity(TrainingPlan plan, boolean isNew) {
		return new TrainingPlanJpaEntity(
				plan.id().value(),
				plan.athleteId().value(),
				plan.athleteSportId() == null ? null : plan.athleteSportId().value(),
				plan.athleteGoalId() == null ? null : plan.athleteGoalId().value(),
				plan.name(),
				plan.normalizedName(),
				plan.description(),
				plan.type(),
				plan.customTypeName(),
				plan.status(),
				plan.startDate(),
				plan.endDate(),
				plan.scheduleStartDate(),
				plan.scheduleEndDate(),
				plan.scheduleTimezone(),
				plan.scheduleStatus(),
				plan.recurrenceMode(),
				plan.scheduleGeneratedThrough(),
				plan.scheduleActivatedAt(),
				plan.schedulePausedAt(),
				plan.createdAt(),
				plan.updatedAt(),
				plan.version(),
				isNew);
	}

	static TrainingPlan toDomain(TrainingPlanJpaEntity entity) {
		return TrainingPlan.rehydrate(
				TrainingPlanId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getAthleteSportId() == null ? null : AthleteSportId.of(entity.getAthleteSportId()),
				entity.getAthleteGoalId() == null ? null : AthleteGoalId.of(entity.getAthleteGoalId()),
				entity.getName(),
				entity.getNormalizedName(),
				entity.getDescription(),
				entity.getPlanType(),
				entity.getCustomTypeName(),
				entity.getStatus(),
				entity.getStartDate(),
				entity.getEndDate(),
				entity.getScheduleStartDate(),
				entity.getScheduleEndDate(),
				entity.getScheduleTimezone(),
				entity.getScheduleStatus(),
				entity.getRecurrenceMode(),
				entity.getScheduleGeneratedThrough(),
				entity.getScheduleActivatedAt(),
				entity.getSchedulePausedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
