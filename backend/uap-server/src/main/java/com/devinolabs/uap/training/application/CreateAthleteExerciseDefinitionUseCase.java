package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;

/**
 * Creates a movement the athlete trains but which is not in the shared catalogue.
 *
 * <p>The athlete row is locked for the duration so two concurrent requests cannot both pass the
 * duplicate-name check; the unique index on the active name is the second line of defence.
 */
@Service
public class CreateAthleteExerciseDefinitionUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final Clock clock;

	public CreateAthleteExerciseDefinitionUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public ExerciseDefinitionResult execute(AccountId accountId, String canonicalName) {
		AthleteRef athlete = ExerciseDefinitionSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		ExerciseDefinitionSupport.assertNoActiveDuplicate(
				exerciseDefinitionRepository, athleteId, canonicalName, null);
		ExerciseDefinition definition = ExerciseDefinition.createAthleteCustom(
				ExerciseDefinitionId.generate(), athleteId, canonicalName, clock);
		return ExerciseDefinitionSupport.toResult(exerciseDefinitionRepository.save(definition));
	}

}
