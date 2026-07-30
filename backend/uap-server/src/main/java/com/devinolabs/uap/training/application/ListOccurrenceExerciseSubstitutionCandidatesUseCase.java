package com.devinolabs.uap.training.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentCompatibilityEvaluator;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceEnvironmentSnapshot;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Service
public class ListOccurrenceExerciseSubstitutionCandidatesUseCase {
	private static final Comparator<ExerciseSubstitutionCompatibility> COMPATIBILITY_ORDER =
			Comparator.comparingInt(ListOccurrenceExerciseSubstitutionCandidatesUseCase::compatibilityRank);

	private final AthleteContextPort athleteContextPort;
	private final TrainingPlanRepository trainingPlanRepository;
	private final WorkoutDayRepository workoutDayRepository;
	private final WorkoutOccurrenceRepository workoutOccurrenceRepository;
	private final WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;
	private final WorkoutExerciseSetRepository workoutExerciseSetRepository;
	private final ExerciseDefinitionRepository exerciseDefinitionRepository;
	private final ExerciseSubstitutionRelationshipRepository relationshipRepository;

	public ListOccurrenceExerciseSubstitutionCandidatesUseCase(
			AthleteContextPort athleteContextPort,
			TrainingPlanRepository trainingPlanRepository,
			WorkoutDayRepository workoutDayRepository,
			WorkoutOccurrenceRepository workoutOccurrenceRepository,
			WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository,
			WorkoutExerciseSetRepository workoutExerciseSetRepository,
			ExerciseDefinitionRepository exerciseDefinitionRepository,
			ExerciseSubstitutionRelationshipRepository relationshipRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.trainingPlanRepository = Objects.requireNonNull(trainingPlanRepository);
		this.workoutDayRepository = Objects.requireNonNull(workoutDayRepository);
		this.workoutOccurrenceRepository = Objects.requireNonNull(workoutOccurrenceRepository);
		this.workoutExerciseExecutionRepository = Objects.requireNonNull(workoutExerciseExecutionRepository);
		this.workoutExerciseSetRepository = Objects.requireNonNull(workoutExerciseSetRepository);
		this.exerciseDefinitionRepository = Objects.requireNonNull(exerciseDefinitionRepository);
		this.relationshipRepository = Objects.requireNonNull(relationshipRepository);
	}

	@Transactional(readOnly = true)
	public List<OccurrenceSubstitutionCandidateResult> execute(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId) {
		AthleteRef athlete = WorkoutExerciseExecutionSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		TrainingPlan plan = WorkoutExerciseExecutionSupport.requirePlan(trainingPlanRepository, athleteId, planId);
		WorkoutDay day = WorkoutExerciseExecutionSupport.requireOwnedDay(
				workoutDayRepository, plan.id(), athleteId, dayId);
		WorkoutOccurrence occurrence = WorkoutExerciseExecutionSupport.requireOwnedOccurrence(
				workoutOccurrenceRepository, occurrenceId, day.id(), athleteId);
		WorkoutExerciseExecution execution = WorkoutExerciseExecutionSupport.requireOwnedExecution(
				workoutExerciseExecutionRepository, executionId, occurrence.id(), day.id(), athleteId);
		List<WorkoutExerciseSet> sets = workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
				execution.id(), athleteId);
		WorkoutExerciseExecutionSupport.requireSubstitutable(execution, sets);
		ExerciseDefinitionId sourceDefinitionId = execution.performedExerciseDefinitionId();
		List<EquipmentType> availableEquipment = WorkoutOccurrenceEnvironmentSupport.resolveEquipmentFilter(occurrence);
		WorkoutOccurrenceEnvironmentSnapshot contextSnapshot =
				WorkoutOccurrenceEnvironmentSupport.resolveSubstitutionContextSnapshot(occurrence);
		WorkoutOccurrenceEnvironmentContextResult environmentContext =
				WorkoutOccurrenceEnvironmentSupport.toContextResult(
						contextSnapshot, occurrence.environmentSelectedAt());
		List<ExerciseSubstitutionRelationship> relationships = relationshipRepository.findActiveBySourceDefinitionId(
				sourceDefinitionId, athleteId);
		if (relationships.isEmpty()) {
			return List.of();
		}
		List<ExerciseDefinitionId> targetIds = relationships.stream()
				.map(ExerciseSubstitutionRelationship::targetExerciseDefinitionId)
				.distinct()
				.toList();
		Map<ExerciseDefinitionId, ExerciseDefinition> targetsById = exerciseDefinitionRepository
				.findAllByIds(targetIds)
				.stream()
				.collect(Collectors.toMap(ExerciseDefinition::id, Function.identity()));
		List<OccurrenceSubstitutionCandidateResult> candidates = new ArrayList<>();
		for (ExerciseSubstitutionRelationship relationship : relationships) {
			ExerciseDefinition target = targetsById.get(relationship.targetExerciseDefinitionId());
			if (target == null || !ExerciseDefinitionAccessPolicy.isSelectableForPrescription(athleteId, target)) {
				continue;
			}
			if (!EquipmentCompatibilityEvaluator.isCompatible(
					target.metadata().requiredEquipment(), availableEquipment)) {
				continue;
			}
			candidates.add(new OccurrenceSubstitutionCandidateResult(
					relationship.id(),
					target.id(),
					target.canonicalName(),
					relationship.relationshipType(),
					relationship.compatibilityLevel(),
					relationship.rationale(),
					environmentContext));
		}
		candidates.sort(Comparator
				.comparing(OccurrenceSubstitutionCandidateResult::compatibilityLevel, COMPATIBILITY_ORDER)
				.thenComparing(OccurrenceSubstitutionCandidateResult::relationshipType)
				.thenComparing(OccurrenceSubstitutionCandidateResult::targetCanonicalName,
						String.CASE_INSENSITIVE_ORDER)
				.thenComparing(candidate -> candidate.targetExerciseDefinitionId().value()));
		return candidates;
	}

	private static int compatibilityRank(ExerciseSubstitutionCompatibility compatibility) {
		return switch (compatibility) {
			case HIGH -> 0;
			case MODERATE -> 1;
			case CONDITIONAL -> 2;
		};
	}
}
