package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalId;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalItemId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistory;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistoryId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
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
				entry.substitutionRelationshipId() == null
						? null
						: entry.substitutionRelationshipId().value(),
				entry.relationshipTypeSnapshot(),
				entry.compatibilitySnapshot(),
				entry.trainingEnvironmentId() == null ? null : entry.trainingEnvironmentId().value(),
				entry.trainingEnvironmentNameSnapshot(),
				new LinkedHashSet<>(entry.availableEquipmentSnapshot()),
				entry.workoutAdaptationProposalId() == null ? null : entry.workoutAdaptationProposalId().value(),
				entry.workoutAdaptationProposalItemId() == null ? null : entry.workoutAdaptationProposalItemId().value(),
				entry.adaptationDecisionSnapshot(),
				entry.reverted(),
				entry.changedAt(),
				entry.createdAt(),
				isNew);
	}

	static WorkoutExerciseSubstitutionHistory toDomain(WorkoutExerciseSubstitutionHistoryJpaEntity entity) {
		List<EquipmentType> equipment = new ArrayList<>(entity.getAvailableEquipmentSnapshot());
		equipment.sort(Comparator.comparingInt(Enum::ordinal));
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
				entity.getSubstitutionRelationshipId() == null
						? null
						: ExerciseSubstitutionRelationshipId.of(entity.getSubstitutionRelationshipId()),
				entity.getRelationshipTypeSnapshot(),
				entity.getCompatibilitySnapshot(),
				entity.getTrainingEnvironmentId() == null
						? null
						: TrainingEnvironmentId.of(entity.getTrainingEnvironmentId()),
				entity.getTrainingEnvironmentNameSnapshot(),
				equipment,
				entity.getWorkoutAdaptationProposalId() == null
						? null
						: WorkoutAdaptationProposalId.of(entity.getWorkoutAdaptationProposalId()),
				entity.getWorkoutAdaptationProposalItemId() == null
						? null
						: WorkoutAdaptationProposalItemId.of(entity.getWorkoutAdaptationProposalItemId()),
				entity.getAdaptationDecisionSnapshot(),
				entity.isReverted(),
				entity.getChangedAt(),
				entity.getCreatedAt());
	}

}
