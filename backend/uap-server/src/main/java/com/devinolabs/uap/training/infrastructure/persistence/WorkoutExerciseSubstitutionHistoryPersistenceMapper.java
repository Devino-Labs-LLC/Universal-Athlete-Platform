package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistory;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistoryId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

final class WorkoutExerciseSubstitutionHistoryPersistenceMapper {

	private WorkoutExerciseSubstitutionHistoryPersistenceMapper() {
	}

	static WorkoutExerciseSubstitutionHistoryJpaEntity toEntity(
			WorkoutExerciseSubstitutionHistory entry,
			boolean isNew) {
		return new WorkoutExerciseSubstitutionHistoryJpaEntity(
				entry.id().value(),
				entry.athleteId().value(),
				entry.workoutOccurrenceId().value(),
				entry.workoutExerciseExecutionId().value(),
				entry.fromExerciseDefinitionId().value(),
				entry.fromExerciseNameSnapshot(),
				entry.toExerciseDefinitionId().value(),
				entry.toExerciseNameSnapshot(),
				entry.reason(),
				entry.notes(),
				entry.reverted(),
				entry.changedAt(),
				entry.createdAt(),
				isNew);
	}

	static WorkoutExerciseSubstitutionHistory toDomain(WorkoutExerciseSubstitutionHistoryJpaEntity entity) {
		return WorkoutExerciseSubstitutionHistory.rehydrate(
				WorkoutExerciseSubstitutionHistoryId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				WorkoutOccurrenceId.of(entity.getWorkoutOccurrenceId()),
				WorkoutExerciseExecutionId.of(entity.getWorkoutExerciseExecutionId()),
				ExerciseDefinitionId.of(entity.getFromExerciseDefinitionId()),
				entity.getFromExerciseNameSnapshot(),
				ExerciseDefinitionId.of(entity.getToExerciseDefinitionId()),
				entity.getToExerciseNameSnapshot(),
				entity.getReason(),
				entity.getNotes(),
				entity.isReverted(),
				entity.getChangedAt(),
				entity.getCreatedAt());
	}

}
