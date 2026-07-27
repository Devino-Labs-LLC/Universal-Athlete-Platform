package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;

/**
 * The exercise picker: active SYSTEM definitions plus the athlete's own active custom definitions.
 *
 * <p>Archived definitions are excluded because they must not be prescribed again, even though the
 * history recorded under them is still readable.
 */
@Service
public class ListAccessibleExerciseDefinitionsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;

	public ListAccessibleExerciseDefinitionsUseCase(
			AthleteContextPort athleteContextPort,
			ExerciseDefinitionRepository exerciseDefinitionRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
	}

	@Transactional(readOnly = true)
	public ExerciseDefinitionPageResult execute(
			AccountId accountId,
			String nameContains,
			ExerciseDefinitionScope scope,
			Integer page,
			Integer size) {
		AthleteRef athlete = ExerciseDefinitionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		return ExerciseDefinitionSupport.toPageResult(exerciseDefinitionRepository.findAccessibleActive(
				athleteId,
				ExerciseDefinitionSupport.normalizeNameFilter(nameContains),
				scope,
				ExerciseDefinitionSupport.requirePage(page),
				ExerciseDefinitionSupport.requireSize(size)));
	}

}
