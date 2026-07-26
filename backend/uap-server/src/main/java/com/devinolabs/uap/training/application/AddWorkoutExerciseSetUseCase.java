package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetType;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class AddWorkoutExerciseSetUseCase {

	private final WorkoutExerciseSetContextLoader contextLoader;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final Clock clock;

	AddWorkoutExerciseSetUseCase(
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
			AddWorkoutExerciseSetCommand command) {
		Objects.requireNonNull(command, "command must not be null");
		WorkoutExerciseSetContextLoader.SetContext context = contextLoader.loadForWrite(
				accountId, planId, dayId, occurrenceId, executionId);
		WorkoutExerciseExecution execution = context.execution();

		List<WorkoutExerciseSet> existing = workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
				execution.id(), context.athleteId());
		if (existing.size() >= WorkoutExerciseSetSupport.MAX_SETS_PER_EXECUTION) {
			throw new WorkoutExerciseSetLimitExceededException(WorkoutExerciseSetSupport.MAX_SETS_PER_EXECUTION);
		}

		Prescription prescription = command.copyFromSetId() == null
				? fromCommand(command, execution)
				: fromTemplate(WorkoutExerciseSetSupport.requireOwnedSet(
						workoutExerciseSetRepository,
						command.copyFromSetId(),
						execution.id(),
						context.athleteId()), command.setType());

		WorkoutExerciseSet added = WorkoutExerciseSet.createAdditional(
				execution.id(),
				execution.workoutOccurrenceId(),
				context.athleteId(),
				WorkoutExerciseSetSupport.nextSetNumber(existing),
				WorkoutExerciseSetSupport.nextDisplayOrder(existing),
				prescription.setType(),
				prescription.minimumReps(),
				prescription.maximumReps(),
				prescription.weight(),
				prescription.weightUnit(),
				prescription.durationSeconds(),
				prescription.distance(),
				prescription.distanceUnit(),
				prescription.targetRpe(),
				prescription.restSeconds(),
				clock);
		return WorkoutExerciseSetSupport.toResult(workoutExerciseSetRepository.save(added));
	}

	private static Prescription fromCommand(AddWorkoutExerciseSetCommand command, WorkoutExerciseExecution execution) {
		if (isEmpty(command)) {
			return new Prescription(
					setTypeOrDefault(command.setType()),
					execution.prescribedMinimumReps(),
					execution.prescribedMaximumReps(),
					execution.prescribedTargetWeight(),
					execution.prescribedWeightUnit(),
					execution.prescribedTargetDurationSeconds(),
					execution.prescribedTargetDistance(),
					execution.prescribedDistanceUnit(),
					execution.prescribedTargetRpe(),
					execution.prescribedTargetRestSeconds());
		}
		return new Prescription(
				setTypeOrDefault(command.setType()),
				command.prescribedMinimumReps(),
				command.prescribedMaximumReps(),
				command.prescribedWeight(),
				command.prescribedWeightUnit(),
				command.prescribedDurationSeconds(),
				command.prescribedDistance(),
				command.prescribedDistanceUnit(),
				command.prescribedTargetRpe(),
				command.prescribedRestSeconds());
	}

	private static Prescription fromTemplate(WorkoutExerciseSet template, WorkoutExerciseSetType setType) {
		return new Prescription(
				setType == null ? template.setType() : setType,
				template.prescribedMinimumReps(),
				template.prescribedMaximumReps(),
				template.prescribedWeight(),
				template.prescribedWeightUnit(),
				template.prescribedDurationSeconds(),
				template.prescribedDistance(),
				template.prescribedDistanceUnit(),
				template.prescribedTargetRpe(),
				template.prescribedRestSeconds());
	}

	private static boolean isEmpty(AddWorkoutExerciseSetCommand command) {
		return command.prescribedMinimumReps() == null
				&& command.prescribedMaximumReps() == null
				&& command.prescribedWeight() == null
				&& command.prescribedWeightUnit() == null
				&& command.prescribedDurationSeconds() == null
				&& command.prescribedDistance() == null
				&& command.prescribedDistanceUnit() == null
				&& command.prescribedTargetRpe() == null
				&& command.prescribedRestSeconds() == null;
	}

	private static WorkoutExerciseSetType setTypeOrDefault(WorkoutExerciseSetType setType) {
		return setType == null ? WorkoutExerciseSetType.WORKING : setType;
	}

	private record Prescription(
			WorkoutExerciseSetType setType,
			Integer minimumReps,
			Integer maximumReps,
			BigDecimal weight,
			WeightUnit weightUnit,
			Integer durationSeconds,
			BigDecimal distance,
			DistanceUnit distanceUnit,
			Integer targetRpe,
			Integer restSeconds) {
	}

}
