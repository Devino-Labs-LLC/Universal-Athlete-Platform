package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;

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
			ExerciseDefinitionCategory category,
			ExerciseMetricMode metricMode,
			MovementPattern movementPattern,
			MuscleGroup muscleGroup,
			EquipmentType equipment,
			ExerciseLaterality laterality,
			ImpactLevel impactLevel,
			ExerciseDifficulty difficulty,
			Integer page,
			Integer size) {
		AthleteRef athlete = ExerciseDefinitionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		ExerciseDefinitionFilters filters = ExerciseDefinitionFilters.of(
				ExerciseDefinitionSupport.normalizeNameFilter(nameContains),
				scope,
				category,
				metricMode,
				movementPattern,
				muscleGroup,
				equipment,
				laterality,
				impactLevel,
				difficulty);
		return ExerciseDefinitionSupport.toPageResult(exerciseDefinitionRepository.findAccessibleActive(
				athleteId,
				filters,
				ExerciseDefinitionSupport.requirePage(page),
				ExerciseDefinitionSupport.requireSize(size)));
	}

}
