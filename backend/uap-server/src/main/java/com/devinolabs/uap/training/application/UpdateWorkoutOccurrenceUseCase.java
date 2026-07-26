package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class UpdateWorkoutOccurrenceUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final Clock clock;

	public UpdateWorkoutOccurrenceUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutOccurrenceDetailResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			UpdateWorkoutOccurrenceCommand command) {
		AthleteRef athlete = WorkoutOccurrenceSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutOccurrenceSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutOccurrenceSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutOccurrenceSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);

		try {
			if (command.scheduledDatePresent()) {
				if (command.scheduledDate() == null) {
					throw new IllegalArgumentException("scheduledDate must not be null");
				}
				if (!command.scheduledDate().equals(occurrence.scheduledDate())) {
					WorkoutOccurrenceSupport.assertUniqueActiveDate(
							workoutOccurrenceRepository,
							day.id(),
							athleteId,
							command.scheduledDate(),
							occurrence.id());
					occurrence.changeScheduledDate(command.scheduledDate(), clock);
				}
			}
			if (command.plannedStartTimePresent() || command.athleteNotesPresent()) {
				var plannedStartTime = command.plannedStartTimePresent()
						? command.plannedStartTime()
						: occurrence.plannedStartTime();
				var athleteNotes = command.athleteNotesPresent()
						? command.athleteNotes()
						: occurrence.athleteNotes();
				occurrence.updateDetails(plannedStartTime, athleteNotes, clock);
			}
		}
		catch (IllegalStateException ex) {
			throw WorkoutOccurrenceSupport.translateStatus(ex);
		}

		WorkoutOccurrence saved = workoutOccurrenceRepository.save(occurrence);
		return WorkoutOccurrenceSupport.toDetailResult(
				saved,
				WorkoutExerciseExecutionSupport.toResults(
						workoutExerciseExecutionRepository.findAllByWorkoutOccurrenceIdAndAthleteId(
								saved.id(), athleteId),
						workoutExerciseSetRepository,
						athleteId));
	}

}
