package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffort;

@Service
public class ListWorkoutSessionEffortRevisionsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutSessionEffortRepository sessionEffortRepository;
	private final WorkoutSessionEffortRevisionRepository revisionRepository;

	public ListWorkoutSessionEffortRevisionsUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutSessionEffortRepository sessionEffortRepository,
			WorkoutSessionEffortRevisionRepository revisionRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.sessionEffortRepository = Objects.requireNonNull(sessionEffortRepository);
		this.revisionRepository = Objects.requireNonNull(revisionRepository);
	}

	@Transactional(readOnly = true)
	public List<WorkoutSessionEffortRevisionResult> execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId) {
		AthleteRef athlete = TrainingPerformanceSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		WorkoutOccurrenceSupport.requireMutablePlan(trainingPlanRepository, athleteId, planId);
		WorkoutOccurrenceSupport.requireOwnedDay(workoutDayRepository, planId, athleteId, dayId);
		WorkoutOccurrenceSupport.requireOwnedOccurrence(workoutOccurrenceRepository, occurrenceId, dayId, athleteId);
		WorkoutSessionEffort effort = sessionEffortRepository.findByOccurrenceIdAndAthleteId(occurrenceId, athleteId)
				.orElseThrow(WorkoutSessionEffortNotFoundException::new);
		return revisionRepository.findAllByEffortIdAndAthleteIdOrderByRevisionNumber(effort.id(), athleteId)
				.stream()
				.map(WorkoutSessionEffortRevisionResult::from)
				.toList();
	}
}
