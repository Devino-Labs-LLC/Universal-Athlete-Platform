package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class UpdateWorkoutExerciseSetUseCase {

	private final WorkoutExerciseSetContextLoader contextLoader;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final Clock clock;

	UpdateWorkoutExerciseSetUseCase(
			WorkoutExerciseSetContextLoader contextLoader,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			Clock clock) {
		this.contextLoader = Objects.requireNonNull(contextLoader);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutExerciseSetResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			WorkoutExerciseSetId setId,
			UpdateWorkoutExerciseSetCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		WorkoutExerciseSetContextLoader.SetContext context = command.touchesActuals()
				? contextLoader.loadForWriteAndPromoteParents(accountId, planId, dayId, occurrenceId, executionId)
				: contextLoader.loadForWrite(accountId, planId, dayId, occurrenceId, executionId);
		WorkoutExerciseSet set = WorkoutExerciseSetSupport.requireOwnedSet(
				workoutExerciseSetRepository, setId, context.execution().id(), context.athleteId());

		try {
			set.requireMutable();
			if (command.touchesActuals()) {
				// Logging against an untouched set opens it; completion always stays explicit.
				if (set.status() == WorkoutExerciseSetStatus.NOT_STARTED) {
					set.start(clock);
				}
				Integer actualReps = command.actualRepsPresent() ? command.actualReps() : set.actualReps();
				BigDecimal actualWeight = command.actualWeightPresent() ? command.actualWeight() : set.actualWeight();
				WeightUnit actualWeightUnit = command.actualWeightUnitPresent()
						? command.actualWeightUnit()
						: set.actualWeightUnit();
				Integer actualDurationSeconds = command.actualDurationSecondsPresent()
						? command.actualDurationSeconds()
						: set.actualDurationSeconds();
				BigDecimal actualDistance = command.actualDistancePresent()
						? command.actualDistance()
						: set.actualDistance();
				DistanceUnit actualDistanceUnit = command.actualDistanceUnitPresent()
						? command.actualDistanceUnit()
						: set.actualDistanceUnit();
				Integer actualRestSeconds = command.actualRestSecondsPresent()
						? command.actualRestSeconds()
						: set.actualRestSeconds();
				BigDecimal actualRpe = command.actualRpePresent() ? command.actualRpe() : set.actualRpe();
				set.updateActuals(
						actualReps,
						actualWeight,
						actualWeightUnit,
						actualDurationSeconds,
						actualDistance,
						actualDistanceUnit,
						actualRestSeconds,
						actualRpe,
						clock);
			}
			if (command.setTypePresent()) {
				set.changeSetType(command.setType(), clock);
			}
			if (command.athleteNotesPresent()) {
				set.updateNotes(command.athleteNotes(), clock);
			}
		}
		catch (IllegalStateException ex) {
			throw WorkoutExerciseSetSupport.translateStatus(ex);
		}

		return WorkoutExerciseSetSupport.toResult(workoutExerciseSetRepository.save(set));
	}

}
