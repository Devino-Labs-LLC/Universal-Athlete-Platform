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
 * Renames one of the athlete's own definitions.
 *
 * <p>The identity is untouched, so every execution and personal record already recorded stays
 * attached; only newly generated executions pick up the new name snapshot.
 */
@Service
public class UpdateAthleteExerciseDefinitionUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final Clock clock;

	public UpdateAthleteExerciseDefinitionUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public ExerciseDefinitionResult execute(
			AccountId accountId,
			ExerciseDefinitionId definitionId,
			String canonicalName) {
		AthleteRef athlete = ExerciseDefinitionSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		ExerciseDefinition definition = ExerciseDefinitionSupport.requireOwnCustom(
				exerciseDefinitionRepository, athleteId, definitionId);
		if (!definition.active()) {
			throw new ExerciseDefinitionArchivedException("Archived exercise definitions cannot be renamed");
		}
		ExerciseDefinitionSupport.assertNoActiveDuplicate(
				exerciseDefinitionRepository, athleteId, canonicalName, definition.id());
		definition.rename(canonicalName, clock);
		return ExerciseDefinitionSupport.toResult(exerciseDefinitionRepository.save(definition));
	}

}
