package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;

@Service
public class GetExerciseDefinitionUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;

	public GetExerciseDefinitionUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseDefinitionRepository exerciseDefinitionRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
	}

	@Transactional(readOnly = true)
	public ExerciseDefinitionResult execute(AccountId accountId, ExerciseDefinitionId definitionId) {
		AthleteRef athlete = ExerciseDefinitionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		return ExerciseDefinitionSupport.toResult(ExerciseDefinitionSupport.requireAccessible(
				exerciseDefinitionRepository, athleteId, definitionId));
	}

}
