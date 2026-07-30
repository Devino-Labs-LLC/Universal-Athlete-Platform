package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutGenerationKey;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceEnvironmentSnapshot;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

final class WorkoutOccurrencePersistenceMapper {

	private WorkoutOccurrencePersistenceMapper() {
	}

	static WorkoutOccurrenceJpaEntity toEntity(WorkoutOccurrence occurrence, boolean isNew) {
		WorkoutOccurrenceJpaEntity entity = new WorkoutOccurrenceJpaEntity(
				occurrence.id().value(),
				occurrence.trainingPlanId().value(),
				occurrence.workoutDayId().value(),
				occurrence.athleteId().value(),
				occurrence.scheduledDate(),
				occurrence.plannedStartTime(),
				occurrence.startedAt(),
				occurrence.completedAt(),
				occurrence.status(),
				occurrence.athleteNotes(),
				occurrence.origin(),
				occurrence.generationKey() == null ? null : occurrence.generationKey().value(),
				occurrence.originalScheduledDate(),
				occurrence.manuallyRescheduled(),
				snapshotId(occurrence.plannedEnvironment()),
				snapshotName(occurrence.plannedEnvironment()),
				snapshotId(occurrence.actualEnvironment()),
				snapshotName(occurrence.actualEnvironment()),
				occurrence.environmentSelectedAt(),
				occurrence.createdAt(),
				occurrence.updatedAt(),
				occurrence.version(),
				toEquipmentSet(occurrence.plannedEnvironment()),
				toEquipmentSet(occurrence.actualEnvironment()),
				isNew);
		return entity;
	}

	static WorkoutOccurrence toDomain(WorkoutOccurrenceJpaEntity entity) {
		return WorkoutOccurrence.rehydrate(
				WorkoutOccurrenceId.of(entity.getId()),
				TrainingPlanId.of(entity.getTrainingPlanId()),
				WorkoutDayId.of(entity.getWorkoutDayId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getScheduledDate(),
				entity.getPlannedStartTime(),
				entity.getStartedAt(),
				entity.getCompletedAt(),
				entity.getStatus(),
				entity.getAthleteNotes(),
				entity.getOrigin(),
				WorkoutGenerationKey.ofNullable(entity.getGenerationKey()),
				entity.getOriginalScheduledDate(),
				entity.isManuallyRescheduled(),
				toSnapshot(
						entity.getPlannedTrainingEnvironmentId(),
						entity.getPlannedTrainingEnvironmentNameSnapshot(),
						entity.getPlannedEquipmentSnapshot()),
				toSnapshot(
						entity.getActualTrainingEnvironmentId(),
						entity.getActualTrainingEnvironmentNameSnapshot(),
						entity.getActualEquipmentSnapshot()),
				entity.getEnvironmentSelectedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

	private static UUID snapshotId(WorkoutOccurrenceEnvironmentSnapshot snapshot) {
		return snapshot == null ? null : snapshot.trainingEnvironmentId().value();
	}

	private static String snapshotName(WorkoutOccurrenceEnvironmentSnapshot snapshot) {
		return snapshot == null ? null : snapshot.nameSnapshot();
	}

	private static Set<EquipmentType> toEquipmentSet(WorkoutOccurrenceEnvironmentSnapshot snapshot) {
		if (snapshot == null) {
			return new LinkedHashSet<>();
		}
		return new LinkedHashSet<>(snapshot.availableEquipmentSnapshot());
	}

	private static WorkoutOccurrenceEnvironmentSnapshot toSnapshot(
			UUID environmentId,
			String nameSnapshot,
			Set<EquipmentType> equipment) {
		if (environmentId == null || nameSnapshot == null) {
			return null;
		}
		List<EquipmentType> ordered = new ArrayList<>(equipment);
		ordered.sort(Comparator.comparingInt(Enum::ordinal));
		return WorkoutOccurrenceEnvironmentSnapshot.of(
				TrainingEnvironmentId.of(environmentId),
				nameSnapshot,
				ordered);
	}

}
