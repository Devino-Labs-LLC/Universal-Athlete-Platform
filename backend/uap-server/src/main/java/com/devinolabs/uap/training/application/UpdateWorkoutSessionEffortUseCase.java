package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.SessionRpe;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;
import com.devinolabs.uap.training.domain.WorkoutSessionEffort;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortRevision;

@Service
public class UpdateWorkoutSessionEffortUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutSessionEffortRepository sessionEffortRepository;
	private final WorkoutSessionEffortRevisionRepository revisionRepository;
	private final WorkoutLoadCalculationSupport loadCalculationSupport;
	private final java.time.Clock clock;

	public UpdateWorkoutSessionEffortUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutSessionEffortRepository sessionEffortRepository,
			WorkoutSessionEffortRevisionRepository revisionRepository,
			WorkoutLoadCalculationSupport loadCalculationSupport,
			java.time.Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.sessionEffortRepository = Objects.requireNonNull(sessionEffortRepository);
		this.revisionRepository = Objects.requireNonNull(revisionRepository);
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
		if (occurrence.status() != WorkoutOccurrenceStatus.COMPLETED) {
			throw new WorkoutSessionEffortNotAllowedException();
		}
		WorkoutSessionEffort effort = sessionEffortRepository
				.findByOccurrenceIdAndAthleteId(occurrence.id(), athleteId)
				.orElseThrow(WorkoutSessionEffortNotFoundException::new);
		try {
			int nextRevision = revisionRepository.countByEffortId(effort.id()) + 1;
			WorkoutSessionEffortRevision revision = effort.update(
					SessionRpe.of(sessionRpe),
					sessionDurationMinutes,
					perceivedNotes,
					occurrence,
					nextRevision,
					clock);
			revisionRepository.save(revision);
			WorkoutSessionEffort saved = sessionEffortRepository.save(effort);
			loadCalculationSupport.calculateAndPersist(
					occurrence, athleteId, plan.id(), day.id(), saved, saved.updatedAt());
			return WorkoutSessionEffortResult.from(saved);
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			throw new WorkoutSessionEffortNotAccessibleException();
		}
	}
}
