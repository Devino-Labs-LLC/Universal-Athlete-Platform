package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionMetadata;

/**
 * Renames or reclassifies one of the athlete's own definitions.
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
			UpdateAthleteExerciseDefinitionCommand command) {
		AthleteRef athlete = ExerciseDefinitionSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		ExerciseDefinition definition = ExerciseDefinitionSupport.requireOwnCustom(
				exerciseDefinitionRepository, athleteId, definitionId);
		if (!definition.active()) {
			throw new ExerciseDefinitionArchivedException("Archived exercise definitions cannot be updated");
		}
		if (command.canonicalNamePresent()) {
			if (command.canonicalName() == null || command.canonicalName().isBlank()) {
				throw new IllegalArgumentException("canonicalName must not be blank");
			}
			ExerciseDefinitionSupport.assertNoActiveDuplicate(
					exerciseDefinitionRepository, athleteId, command.canonicalName(), definition.id());
			definition.rename(command.canonicalName(), clock);
		}
		if (command.hasMetadataPatch()) {
			definition.updateMetadata(mergeMetadata(definition.metadata(), command), clock);
		}
		return ExerciseDefinitionSupport.toResult(exerciseDefinitionRepository.save(definition));
	}

	private static ExerciseDefinitionMetadata mergeMetadata(
			ExerciseDefinitionMetadata current,
			UpdateAthleteExerciseDefinitionCommand command) {
		return ExerciseDefinitionMetadata.of(
				command.categoryPresent() ? command.category() : current.category(),
				command.metricModePresent() ? command.metricMode() : current.metricMode(),
				command.primaryMovementPatternPresent()
						? command.primaryMovementPattern()
						: current.primaryMovementPattern(),
				command.secondaryMovementPatternsPresent()
						? nullSafeList(command.secondaryMovementPatterns())
						: current.secondaryMovementPatterns(),
				command.primaryMuscleGroupsPresent()
						? nullSafeList(command.primaryMuscleGroups())
						: current.primaryMuscleGroups(),
				command.secondaryMuscleGroupsPresent()
						? nullSafeList(command.secondaryMuscleGroups())
						: current.secondaryMuscleGroups(),
				command.requiredEquipmentPresent()
						? nullSafeList(command.requiredEquipment())
						: current.requiredEquipment(),
				command.optionalEquipmentPresent()
						? nullSafeList(command.optionalEquipment())
						: current.optionalEquipment(),
				command.lateralityPresent() ? command.laterality() : current.laterality(),
				command.kineticChainTypePresent() ? command.kineticChainType() : current.kineticChainType(),
				command.impactLevelPresent() ? command.impactLevel() : current.impactLevel(),
				command.difficultyPresent() ? command.difficulty() : current.difficulty());
	}

	private static <T> List<T> nullSafeList(List<T> values) {
		return values == null ? List.of() : values;
	}

}
