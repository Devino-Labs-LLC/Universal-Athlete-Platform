package com.devinolabs.uap.training.infrastructure.persistence;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

final class WorkoutExerciseExecutionPersistenceMapper {

	private WorkoutExerciseExecutionPersistenceMapper() {
	}

	static WorkoutExerciseExecutionJpaEntity toEntity(WorkoutExerciseExecution execution, boolean isNew) {
		return new WorkoutExerciseExecutionJpaEntity(
				execution.id().value(),
				execution.workoutOccurrenceId().value(),
				execution.sourceWorkoutExerciseId().value(),
				execution.prescribedExerciseDefinitionId().value(),
				execution.prescribedExerciseNameSnapshot(),
				execution.performedExerciseDefinitionId().value(),
				execution.performedExerciseNameSnapshot(),
				execution.exercisePerformanceKey().value(),
				execution.substitutionReason(),
				execution.substitutionNotes(),
				execution.substitutedAt(),
				execution.athleteId().value(),
				execution.displayOrder(),
				execution.category(),
				execution.type(),
				execution.prescribedSets(),
				execution.prescribedMinimumReps(),
				execution.prescribedMaximumReps(),
				execution.prescribedTargetWeight(),
				execution.prescribedWeightUnit(),
				execution.prescribedTargetDurationSeconds(),
				execution.prescribedTargetDistance(),
				execution.prescribedDistanceUnit(),
				execution.prescribedTargetRestSeconds(),
				execution.prescribedTargetRpe(),
				execution.prescribedTempo(),
				execution.prescribedCoachingNotes(),
				execution.status(),
				execution.actualSets(),
				execution.actualReps(),
				execution.actualWeight(),
				execution.weightUnit(),
				execution.actualDurationSeconds(),
				execution.actualDistance(),
				execution.distanceUnit(),
				execution.actualRestSeconds(),
				execution.actualRpe(),
				execution.startedAt(),
				execution.completedAt(),
				execution.athleteNotes(),
				execution.createdAt(),
				execution.updatedAt(),
				execution.version(),
				isNew);
	}

	static WorkoutExerciseExecution toDomain(WorkoutExerciseExecutionJpaEntity entity) {
		return WorkoutExerciseExecution.rehydrate(
				WorkoutExerciseExecutionId.of(entity.getId()),
				WorkoutOccurrenceId.of(entity.getWorkoutOccurrenceId()),
				WorkoutExerciseId.of(entity.getSourceWorkoutExerciseId()),
				ExerciseDefinitionId.of(entity.getPrescribedExerciseDefinitionId()),
				entity.getPrescribedExerciseNameSnapshot(),
				ExerciseDefinitionId.of(entity.getPerformedExerciseDefinitionId()),
				entity.getPerformedExerciseNameSnapshot(),
				ExercisePerformanceKey.of(entity.getExercisePerformanceKey()),
				entity.getSubstitutionReason(),
				entity.getSubstitutionNotes(),
				entity.getSubstitutedAt(),
				AthleteId.of(entity.getAthleteId()),
				entity.getDisplayOrder(),
				entity.getCategory(),
				entity.getType(),
				entity.getPrescribedSets(),
				entity.getPrescribedMinimumReps(),
				entity.getPrescribedMaximumReps(),
				entity.getPrescribedTargetWeight(),
				entity.getPrescribedWeightUnit(),
				entity.getPrescribedTargetDurationSeconds(),
				entity.getPrescribedTargetDistance(),
				entity.getPrescribedDistanceUnit(),
				entity.getPrescribedTargetRestSeconds(),
				entity.getPrescribedTargetRpe(),
				entity.getPrescribedTempo(),
				entity.getPrescribedCoachingNotes(),
				entity.getStatus(),
				entity.getActualSets(),
				entity.getActualReps(),
				entity.getActualWeight(),
				entity.getWeightUnit(),
				entity.getActualDurationSeconds(),
				entity.getActualDistance(),
				entity.getDistanceUnit(),
				entity.getActualRestSeconds(),
				entity.getActualRpe(),
				entity.getStartedAt(),
				entity.getCompletedAt(),
				entity.getAthleteNotes(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
