package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceEnvironmentSnapshot;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

final class WorkoutOccurrenceEnvironmentSupport {

	private WorkoutOccurrenceEnvironmentSupport() {
	}

	static WorkoutOccurrenceEnvironmentContextResult toContextResult(
			WorkoutOccurrenceEnvironmentSnapshot snapshot,
			java.time.Instant selectedAt) {
		if (snapshot == null) {
			return null;
		}
		return new WorkoutOccurrenceEnvironmentContextResult(
				snapshot.trainingEnvironmentId(),
				snapshot.nameSnapshot(),
				snapshot.availableEquipmentSnapshot(),
				selectedAt);
	}

	static WorkoutOccurrenceEnvironmentDetailResult toDetailResult(WorkoutOccurrence occurrence) {
		return new WorkoutOccurrenceEnvironmentDetailResult(
				toContextResult(occurrence.plannedEnvironment(), null),
				toContextResult(occurrence.actualEnvironment(), occurrence.environmentSelectedAt()));
	}

	static List<com.devinolabs.uap.training.domain.EquipmentType> resolveEquipmentFilter(WorkoutOccurrence occurrence) {
		if (occurrence.actualEnvironment() != null) {
			return occurrence.actualEnvironment().availableEquipmentSnapshot();
		}
		if (occurrence.plannedEnvironment() != null) {
			return occurrence.plannedEnvironment().availableEquipmentSnapshot();
		}
		return List.of();
	}

	static WorkoutOccurrenceEnvironmentSnapshot resolveSubstitutionContextSnapshot(WorkoutOccurrence occurrence) {
		if (occurrence.actualEnvironment() != null) {
			return occurrence.actualEnvironment();
		}
		return occurrence.plannedEnvironment();
	}

	static void requireEnvironmentMutable(
			WorkoutOccurrence occurrence,
			List<WorkoutExerciseExecution> executions,
			List<WorkoutExerciseSet> sets) {
		Objects.requireNonNull(occurrence, "occurrence must not be null");
		if (occurrence.status() == WorkoutOccurrenceStatus.COMPLETED
				|| occurrence.status() == WorkoutOccurrenceStatus.SKIPPED
				|| occurrence.status() == WorkoutOccurrenceStatus.CANCELLED) {
			throw new WorkoutOccurrenceEnvironmentLockedException();
		}
		if (executions.stream().anyMatch(execution -> execution.status() != WorkoutExerciseExecutionStatus.NOT_STARTED)) {
			throw new WorkoutOccurrenceEnvironmentLockedException();
		}
		if (sets.stream().anyMatch(set -> set.status() != WorkoutExerciseSetStatus.NOT_STARTED)) {
			throw new WorkoutOccurrenceEnvironmentLockedException();
		}
	}

}
