package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.SessionEffortSource;
import com.devinolabs.uap.training.domain.SessionRpe;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;
import com.devinolabs.uap.training.domain.WorkoutSessionEffort;

@Service
public class SubmitWorkoutSessionEffortUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutSessionEffortRepository sessionEffortRepository;
	private final WorkoutLoadCalculationSupport loadCalculationSupport;
	private final java.time.Clock clock;

	public SubmitWorkoutSessionEffortUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutSessionEffortRepository sessionEffortRepository,
			WorkoutLoadCalculationSupport loadCalculationSupport,
			java.time.Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.sessionEffortRepository = Objects.requireNonNull(sessionEffortRepository);
		this.loadCalculationSupport = Objects.requireNonNull(loadCalculationSupport);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public WorkoutSessionEffortResult execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			BigDecimal sessionRpe,
			Integer sessionDurationMinutes,
			String perceivedNotes) {
		AthleteRef athlete = TrainingPerformanceSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutOccurrenceSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutOccurrenceSupport.requireOwnedDay(workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutOccurrenceSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);
		requireCompleted(occurrence);
		if (sessionEffortRepository.existsByOccurrenceId(occurrence.id())) {
			throw new WorkoutSessionEffortAlreadyExistsException();
		}
		WorkoutSessionEffort effort = WorkoutSessionEffort.create(
				athleteId,
				plan.id(),
				day.id(),
				occurrence.id(),
				SessionRpe.of(sessionRpe),
				sessionDurationMinutes,
				perceivedNotes,
				occurrence,
				SessionEffortSource.ATHLETE_REPORTED,
				clock);
		try {
			WorkoutSessionEffort saved = sessionEffortRepository.save(effort);
			loadCalculationSupport.calculateAndPersist(
					occurrence, athleteId, plan.id(), day.id(), saved, saved.updatedAt());
			return WorkoutSessionEffortResult.from(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw new WorkoutSessionEffortAlreadyExistsException();
		}
	}

	private static void requireCompleted(WorkoutOccurrence occurrence) {
		if (occurrence.status() != WorkoutOccurrenceStatus.COMPLETED) {
			throw new WorkoutSessionEffortNotAllowedException();
		}
	}
}
