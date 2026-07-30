package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseEnvironmentCompatibility;
import com.devinolabs.uap.training.domain.ExerciseEnvironmentCompatibilityEvaluator;
import com.devinolabs.uap.training.domain.TrainingEnvironment;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

@Service
public class GetExerciseEnvironmentCompatibilityUseCase {
	private final AthleteContextPort athleteContextPort;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final TrainingEnvironmentRepository trainingEnvironmentRepository;

	public GetExerciseEnvironmentCompatibilityUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			TrainingEnvironmentRepository trainingEnvironmentRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.trainingEnvironmentRepository = Objects.requireNonNull(trainingEnvironmentRepository);
	}

	@Transactional(readOnly = true)
	public ExerciseEnvironmentCompatibilityResult execute(
			AccountId accountId,
			ExerciseDefinitionId exerciseDefinitionId,
			TrainingEnvironmentId environmentId) {
		AthleteRef athlete = ExerciseDefinitionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		ExerciseDefinition exercise = ExerciseDefinitionSupport.requireAccessible(
				exerciseDefinitionRepository, athleteId, exerciseDefinitionId);
		TrainingEnvironment environment = TrainingEnvironmentSupport.requireOwnedActive(
				trainingEnvironmentRepository, athleteId, environmentId);
		ExerciseEnvironmentCompatibility compatibility = ExerciseEnvironmentCompatibilityEvaluator.evaluate(
				exercise.metadata().requiredEquipment(), environment.availableEquipment());
		return new ExerciseEnvironmentCompatibilityResult(
				exercise.id(),
				environment.id(),
				environment.name(),
				compatibility.compatible(),
				compatibility.requiredEquipment(),
				compatibility.availableEquipment(),
				compatibility.missingRequiredEquipment());
	}
}
