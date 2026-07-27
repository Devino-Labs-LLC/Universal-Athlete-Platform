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
 * Retires one of the athlete's own definitions from selection.
 *
 * <p>Definitions are archived rather than deleted: the athlete's history and personal records are
 * keyed by the definition id, and deleting it would orphan them.
 */
@Service
public class ArchiveAthleteExerciseDefinitionUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final Clock clock;

	public ArchiveAthleteExerciseDefinitionUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public ExerciseDefinitionResult execute(AccountId accountId, ExerciseDefinitionId definitionId) {
		AthleteRef athlete = ExerciseDefinitionSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		ExerciseDefinition definition = ExerciseDefinitionSupport.requireOwnCustom(
				exerciseDefinitionRepository, athleteId, definitionId);
		if (!definition.active()) {
			return ExerciseDefinitionSupport.toResult(definition);
		}
		definition.archive(clock);
		return ExerciseDefinitionSupport.toResult(exerciseDefinitionRepository.save(definition));
	}

}
