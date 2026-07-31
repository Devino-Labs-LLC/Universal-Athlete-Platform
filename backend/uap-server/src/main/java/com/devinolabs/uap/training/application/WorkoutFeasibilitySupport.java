package com.devinolabs.uap.training.application;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentCompatibilityEvaluator;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseEnvironmentCompatibility;
import com.devinolabs.uap.training.domain.ExerciseEnvironmentCompatibilityEvaluator;
import com.devinolabs.uap.training.domain.ExerciseFeasibilityStatus;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionSuggestionRanker;
import com.devinolabs.uap.training.domain.FeasibilityEnvironmentContextSource;
import com.devinolabs.uap.training.domain.FeasibilityReasonCode;
import com.devinolabs.uap.training.domain.TrainingEnvironment;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceEnvironmentSnapshot;

final class WorkoutFeasibilitySupport {

	static final int DEFAULT_SUGGESTION_LIMIT = 3;
	static final int MIN_SUGGESTION_LIMIT = 0;
	static final int MAX_SUGGESTION_LIMIT = 10;

	private WorkoutFeasibilitySupport() {
	}

	static int resolveSuggestionLimit(Integer suggestionLimit) {
		int resolved = suggestionLimit == null ? DEFAULT_SUGGESTION_LIMIT : suggestionLimit;
		if (resolved < MIN_SUGGESTION_LIMIT || resolved > MAX_SUGGESTION_LIMIT) {
			throw new InvalidFeasibilitySuggestionLimitException();
		}
		return resolved;
	}

	static void assertEnvironmentMode(TrainingEnvironmentId trainingEnvironmentId, Boolean usePreferredEnvironments) {
		boolean explicit = trainingEnvironmentId != null;
		boolean preferred = Boolean.TRUE.equals(usePreferredEnvironments);
		if (explicit == preferred) {
			throw new InvalidFeasibilityEnvironmentModeException();
		}
	}

	static FeasibilityEnvironmentContextResult resolveExplicitEnvironment(
			TrainingEnvironmentRepository repository,
			AthleteId athleteId,
			TrainingEnvironmentId environmentId) {
		TrainingEnvironment environment = TrainingEnvironmentSupport.requireOwnedActive(
				repository, athleteId, environmentId);
		return fromEnvironment(environment, FeasibilityEnvironmentContextSource.EXPLICIT_ENVIRONMENT, null);
	}

	static FeasibilityEnvironmentContextResult resolvePreferredEnvironment(
			TrainingEnvironmentRepository repository,
			WorkoutDay day,
			TrainingPlan plan,
			AthleteId athleteId) {
		TrainingEnvironmentId overrideId = day.trainingEnvironmentOverrideId();
		if (overrideId != null) {
			TrainingEnvironment override = repository.findOwnedById(overrideId, athleteId).orElse(null);
			if (override != null && override.active()) {
				return fromEnvironment(override, FeasibilityEnvironmentContextSource.DAY_OVERRIDE, null);
			}
		}
		TrainingEnvironmentId planDefaultId = plan.defaultTrainingEnvironmentId();
		if (planDefaultId != null) {
			TrainingEnvironment planDefault = repository.findOwnedById(planDefaultId, athleteId).orElse(null);
			if (planDefault != null && planDefault.active()) {
				return fromEnvironment(planDefault, FeasibilityEnvironmentContextSource.PLAN_DEFAULT, null);
			}
		}
		return repository.findActiveDefaultByAthleteId(athleteId)
				.map(environment -> fromEnvironment(
						environment, FeasibilityEnvironmentContextSource.ATHLETE_DEFAULT, null))
				.orElse(null);
	}

	static FeasibilityEnvironmentContextResult resolveOccurrenceEnvironment(WorkoutOccurrence occurrence) {
		if (occurrence.actualEnvironment() != null) {
			return fromSnapshot(
					occurrence.actualEnvironment(),
					FeasibilityEnvironmentContextSource.OCCURRENCE_ACTUAL_SNAPSHOT,
					occurrence.environmentSelectedAt());
		}
		if (occurrence.plannedEnvironment() != null) {
			return fromSnapshot(
					occurrence.plannedEnvironment(),
					FeasibilityEnvironmentContextSource.OCCURRENCE_PLANNED_SNAPSHOT,
					null);
		}
		return null;
	}

	static Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource(
			ExerciseSubstitutionRelationshipRepository repository,
			List<ExerciseDefinitionId> sourceDefinitionIds,
			AthleteId athleteId) {
		if (sourceDefinitionIds.isEmpty()) {
			return Map.of();
		}
		return repository.findActiveBySourceDefinitionIds(sourceDefinitionIds, athleteId).stream()
				.collect(Collectors.groupingBy(
						ExerciseSubstitutionRelationship::sourceExerciseDefinitionId,
						Collectors.toList()));
	}

	static Map<ExerciseDefinitionId, ExerciseDefinition> loadDefinitions(
			ExerciseDefinitionRepository repository,
			List<ExerciseDefinitionId> definitionIds) {
		if (definitionIds.isEmpty()) {
			return Map.of();
		}
		return repository.findAllByIds(definitionIds).stream()
				.collect(Collectors.toMap(ExerciseDefinition::id, Function.identity()));
	}

	static PrescriptionAnalysis analyzePrescription(
			AthleteId athleteId,
			ExerciseDefinitionId exerciseDefinitionId,
			String exerciseName,
			FeasibilityEnvironmentContextResult environmentContext,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource,
			int suggestionLimit,
			boolean includeAlternatives) {
		ExerciseDefinition definition = definitionsById.get(exerciseDefinitionId);
		if (definition == null) {
			return PrescriptionAnalysis.notAnalyzable(exerciseName);
		}
		if (!ExerciseDefinitionAccessPolicy.isAccessible(athleteId, definition)) {
			return PrescriptionAnalysis.notAccessible(exerciseName);
		}
		if (!definition.active()) {
			return analyzeArchivedPrescription(
					athleteId, definition, exerciseName, environmentContext, relationshipsBySource,
					definitionsById, suggestionLimit, includeAlternatives);
		}
		if (environmentContext == null) {
			return PrescriptionAnalysis.noEnvironmentContext(definition, exerciseName);
		}
		return analyzeActivePrescription(
				athleteId, definition, exerciseName, environmentContext, relationshipsBySource,
				definitionsById, suggestionLimit, includeAlternatives);
	}

	static ExecutionAnalysis analyzeExecution(
			AthleteId athleteId,
			ExerciseDefinitionId prescribedDefinitionId,
			String prescribedName,
			ExerciseDefinitionId performedDefinitionId,
			String performedName,
			boolean substituted,
			FeasibilityEnvironmentContextResult environmentContext,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource,
			int suggestionLimit,
			boolean includeAlternatives) {
		ExerciseDefinition prescribed = definitionsById.get(prescribedDefinitionId);
		ExerciseDefinition performed = definitionsById.get(performedDefinitionId);
		if (prescribed == null || performed == null) {
			return ExecutionAnalysis.notAnalyzable(prescribedName, performedName, substituted);
		}
		if (!ExerciseDefinitionAccessPolicy.isAccessible(athleteId, prescribed)
				|| !ExerciseDefinitionAccessPolicy.isAccessible(athleteId, performed)) {
			return ExecutionAnalysis.notAccessible(prescribedName, performedName, substituted);
		}
		boolean historicalSnapshot = environmentContext != null
				&& (environmentContext.contextSource() == FeasibilityEnvironmentContextSource.OCCURRENCE_ACTUAL_SNAPSHOT
						|| environmentContext.contextSource()
								== FeasibilityEnvironmentContextSource.OCCURRENCE_PLANNED_SNAPSHOT);
		if (!prescribed.active() || !performed.active()) {
			return analyzeArchivedExecution(
					athleteId, prescribed, performed, prescribedName, performedName, substituted,
					environmentContext, relationshipsBySource, definitionsById, suggestionLimit,
					includeAlternatives, historicalSnapshot);
		}
		if (environmentContext == null) {
			return ExecutionAnalysis.noEnvironmentContext(
					prescribed, performed, prescribedName, performedName, substituted);
		}
		List<EquipmentType> availableEquipment = environmentContext.availableEquipment();
		ExerciseEnvironmentCompatibility prescribedCompatibility = ExerciseEnvironmentCompatibilityEvaluator.evaluate(
				prescribed.metadata().requiredEquipment(), availableEquipment);
		ExerciseEnvironmentCompatibility performedCompatibility = ExerciseEnvironmentCompatibilityEvaluator.evaluate(
				performed.metadata().requiredEquipment(), availableEquipment);
		boolean currentExecutionFeasible = performedCompatibility.compatible();
		ExerciseFeasibilityStatus status;
		if (substituted && currentExecutionFeasible) {
			status = ExerciseFeasibilityStatus.FEASIBLE_AS_PERFORMED;
		}
		else if (!substituted && prescribedCompatibility.compatible()) {
			status = ExerciseFeasibilityStatus.FEASIBLE_AS_PRESCRIBED;
		}
		else if (currentExecutionFeasible) {
			status = ExerciseFeasibilityStatus.FEASIBLE_AS_PERFORMED;
		}
		else {
			status = ExerciseFeasibilityStatus.MISSING_REQUIRED_EQUIPMENT;
		}
		SuggestionOutcome suggestions = buildSuggestions(
				athleteId,
				prescribed,
				availableEquipment,
				relationshipsBySource.getOrDefault(prescribed.id(), List.of()),
				definitionsById,
				suggestionLimit);
		FeasibilityReasonCode reasonCode = resolveExecutionReasonCode(
				status, prescribedCompatibility, performedCompatibility, substituted, suggestions,
				historicalSnapshot);
		String reasonSummary = summarizeReason(reasonCode, performedCompatibility.missingRequiredEquipment());
		List<ExerciseSubstitutionSuggestionResult> rankedSuggestions = List.of();
		if (shouldIncludeSuggestions(currentExecutionFeasible, includeAlternatives, suggestionLimit)) {
			rankedSuggestions = rankSuggestions(suggestions.compatible(), prescribed.metadata().difficulty(), suggestionLimit);
		}
		if (!currentExecutionFeasible && suggestions.compatible().isEmpty() && suggestions.totalRelationships() > 0) {
			status = ExerciseFeasibilityStatus.NO_COMPATIBLE_SUBSTITUTION;
		}
		else if (!currentExecutionFeasible && suggestions.totalRelationships() == 0) {
			status = ExerciseFeasibilityStatus.NO_COMPATIBLE_SUBSTITUTION;
		}
		return new ExecutionAnalysis(
				ExerciseCompatibilityDetailResult.from(prescribedCompatibility),
				ExerciseCompatibilityDetailResult.from(performedCompatibility),
				currentExecutionFeasible,
				status,
				reasonCode,
				reasonSummary,
				suggestions.compatible().size(),
				rankedSuggestions,
				!suggestions.compatible().isEmpty());
	}

	private static PrescriptionAnalysis analyzeActivePrescription(
			AthleteId athleteId,
			ExerciseDefinition definition,
			String exerciseName,
			FeasibilityEnvironmentContextResult environmentContext,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			int suggestionLimit,
			boolean includeAlternatives) {
		List<EquipmentType> availableEquipment = environmentContext.availableEquipment();
		ExerciseEnvironmentCompatibility compatibility = ExerciseEnvironmentCompatibilityEvaluator.evaluate(
				definition.metadata().requiredEquipment(), availableEquipment);
		boolean feasible = compatibility.compatible();
		SuggestionOutcome suggestions = buildSuggestions(
				athleteId,
				definition,
				availableEquipment,
				relationshipsBySource.getOrDefault(definition.id(), List.of()),
				definitionsById,
				suggestionLimit);
		ExerciseFeasibilityStatus status = feasible
				? ExerciseFeasibilityStatus.FEASIBLE_AS_PRESCRIBED
				: suggestions.compatible().isEmpty()
						? ExerciseFeasibilityStatus.NO_COMPATIBLE_SUBSTITUTION
						: ExerciseFeasibilityStatus.MISSING_REQUIRED_EQUIPMENT;
		FeasibilityReasonCode reasonCode = resolvePrescriptionReasonCode(feasible, compatibility, suggestions, false);
		String reasonSummary = summarizeReason(reasonCode, compatibility.missingRequiredEquipment());
		List<ExerciseSubstitutionSuggestionResult> rankedSuggestions = List.of();
		if (shouldIncludeSuggestions(feasible, includeAlternatives, suggestionLimit)) {
			rankedSuggestions = rankSuggestions(suggestions.compatible(), definition.metadata().difficulty(), suggestionLimit);
		}
		return new PrescriptionAnalysis(
				feasible,
				ExerciseCompatibilityDetailResult.from(compatibility),
				status,
				reasonCode,
				reasonSummary,
				suggestions.compatible().size(),
				rankedSuggestions,
				!suggestions.compatible().isEmpty());
	}

	private static PrescriptionAnalysis analyzeArchivedPrescription(
			AthleteId athleteId,
			ExerciseDefinition definition,
			String exerciseName,
			FeasibilityEnvironmentContextResult environmentContext,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			int suggestionLimit,
			boolean includeAlternatives) {
		ExerciseCompatibilityDetailResult compatibility = environmentContext == null
				? new ExerciseCompatibilityDetailResult(false, List.of(), List.of(), List.of())
				: ExerciseCompatibilityDetailResult.from(ExerciseEnvironmentCompatibilityEvaluator.evaluate(
						definition.metadata().requiredEquipment(), environmentContext.availableEquipment()));
		return new PrescriptionAnalysis(
				false,
				compatibility,
				ExerciseFeasibilityStatus.ARCHIVED_HISTORICAL_REFERENCE,
				FeasibilityReasonCode.EXERCISE_DEFINITION_ARCHIVED,
				summarizeReason(FeasibilityReasonCode.EXERCISE_DEFINITION_ARCHIVED, compatibility.missingRequiredEquipment()),
				0,
				List.of(),
				false);
	}

	private static ExecutionAnalysis analyzeArchivedExecution(
			AthleteId athleteId,
			ExerciseDefinition prescribed,
			ExerciseDefinition performed,
			String prescribedName,
			String performedName,
			boolean substituted,
			FeasibilityEnvironmentContextResult environmentContext,
			Map<ExerciseDefinitionId, List<ExerciseSubstitutionRelationship>> relationshipsBySource,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			int suggestionLimit,
			boolean includeAlternatives,
			boolean historicalSnapshot) {
		ExerciseCompatibilityDetailResult prescribedCompatibility = environmentContext == null
				? new ExerciseCompatibilityDetailResult(false, List.of(), List.of(), List.of())
				: ExerciseCompatibilityDetailResult.from(ExerciseEnvironmentCompatibilityEvaluator.evaluate(
						prescribed.metadata().requiredEquipment(), environmentContext.availableEquipment()));
		ExerciseCompatibilityDetailResult performedCompatibility = environmentContext == null
				? new ExerciseCompatibilityDetailResult(false, List.of(), List.of(), List.of())
				: ExerciseCompatibilityDetailResult.from(ExerciseEnvironmentCompatibilityEvaluator.evaluate(
						performed.metadata().requiredEquipment(), environmentContext.availableEquipment()));
		return new ExecutionAnalysis(
				prescribedCompatibility,
				performedCompatibility,
				false,
				ExerciseFeasibilityStatus.ARCHIVED_HISTORICAL_REFERENCE,
				FeasibilityReasonCode.EXERCISE_DEFINITION_ARCHIVED,
				summarizeReason(FeasibilityReasonCode.EXERCISE_DEFINITION_ARCHIVED,
						performedCompatibility.missingRequiredEquipment()),
				0,
				List.of(),
				false);
	}

	private static FeasibilityReasonCode resolvePrescriptionReasonCode(
			boolean feasible,
			ExerciseEnvironmentCompatibility compatibility,
			SuggestionOutcome suggestions,
			boolean historicalSnapshot) {
		if (historicalSnapshot) {
			return FeasibilityReasonCode.HISTORICAL_SNAPSHOT_ANALYSIS;
		}
		if (feasible) {
			return compatibility.requiredEquipment().stream().allMatch(type -> type == EquipmentType.BODYWEIGHT)
					? FeasibilityReasonCode.NO_REQUIRED_EQUIPMENT
					: FeasibilityReasonCode.ALL_REQUIRED_EQUIPMENT_AVAILABLE;
		}
		if (!suggestions.compatible().isEmpty()) {
			return FeasibilityReasonCode.COMPATIBLE_SUBSTITUTION_FOUND;
		}
		if (suggestions.totalRelationships() > 0) {
			return FeasibilityReasonCode.RELATIONSHIP_TARGET_REQUIRES_MISSING_EQUIPMENT;
		}
		return FeasibilityReasonCode.NO_ACTIVE_SUBSTITUTION_RELATIONSHIP;
	}

	private static FeasibilityReasonCode resolveExecutionReasonCode(
			ExerciseFeasibilityStatus status,
			ExerciseEnvironmentCompatibility prescribedCompatibility,
			ExerciseEnvironmentCompatibility performedCompatibility,
			boolean substituted,
			SuggestionOutcome suggestions,
			boolean historicalSnapshot) {
		if (historicalSnapshot
				&& (status == ExerciseFeasibilityStatus.FEASIBLE_AS_PERFORMED
						|| status == ExerciseFeasibilityStatus.FEASIBLE_AS_PRESCRIBED)) {
			return FeasibilityReasonCode.HISTORICAL_SNAPSHOT_ANALYSIS;
		}
		if (status == ExerciseFeasibilityStatus.FEASIBLE_AS_PERFORMED
				|| status == ExerciseFeasibilityStatus.FEASIBLE_AS_PRESCRIBED) {
			ExerciseEnvironmentCompatibility relevant = substituted ? performedCompatibility : prescribedCompatibility;
			return relevant.requiredEquipment().stream().allMatch(type -> type == EquipmentType.BODYWEIGHT)
					? FeasibilityReasonCode.NO_REQUIRED_EQUIPMENT
					: FeasibilityReasonCode.ALL_REQUIRED_EQUIPMENT_AVAILABLE;
		}
		if (!suggestions.compatible().isEmpty()) {
			return FeasibilityReasonCode.COMPATIBLE_SUBSTITUTION_FOUND;
		}
		if (suggestions.totalRelationships() > 0) {
			return FeasibilityReasonCode.RELATIONSHIP_TARGET_REQUIRES_MISSING_EQUIPMENT;
		}
		return FeasibilityReasonCode.NO_ACTIVE_SUBSTITUTION_RELATIONSHIP;
	}

	private static String summarizeReason(FeasibilityReasonCode reasonCode, List<EquipmentType> missingEquipment) {
		return switch (reasonCode) {
			case ALL_REQUIRED_EQUIPMENT_AVAILABLE -> "All required equipment is available in the selected environment.";
			case NO_REQUIRED_EQUIPMENT -> "The exercise does not require external equipment.";
			case MISSING_REQUIRED_EQUIPMENT -> "Missing required equipment: " + formatEquipment(missingEquipment);
			case COMPATIBLE_SUBSTITUTION_FOUND -> "Compatible substitution options are available.";
			case NO_ACTIVE_SUBSTITUTION_RELATIONSHIP -> "No active substitution relationships are configured.";
			case RELATIONSHIP_TARGET_REQUIRES_MISSING_EQUIPMENT ->
				"Substitution targets also require unavailable equipment.";
			case EXERCISE_DEFINITION_ARCHIVED -> "The exercise definition is archived and retained for historical reference.";
			case NO_ENVIRONMENT_CONTEXT -> "No training environment context is available for analysis.";
			case EXERCISE_DEFINITION_NOT_ACCESSIBLE -> "The exercise definition is not accessible.";
			case HISTORICAL_SNAPSHOT_ANALYSIS -> "Analysis uses the occurrence environment snapshot.";
		};
	}

	private static String formatEquipment(List<EquipmentType> equipment) {
		if (equipment == null || equipment.isEmpty()) {
		 return "none";
		}
		return equipment.stream().map(Enum::name).collect(Collectors.joining(", "));
	}

	private static boolean shouldIncludeSuggestions(boolean feasible, boolean includeAlternatives, int suggestionLimit) {
		return suggestionLimit > 0 && (!feasible || includeAlternatives);
	}

	private static SuggestionOutcome buildSuggestions(
			AthleteId athleteId,
			ExerciseDefinition source,
			List<EquipmentType> availableEquipment,
			List<ExerciseSubstitutionRelationship> relationships,
			Map<ExerciseDefinitionId, ExerciseDefinition> definitionsById,
			int suggestionLimit) {
		if (relationships.isEmpty()) {
			return new SuggestionOutcome(0, List.of());
		}
		List<RankedSuggestionCandidate> compatible = new ArrayList<>();
		for (ExerciseSubstitutionRelationship relationship : relationships) {
			ExerciseDefinition target = definitionsById.get(relationship.targetExerciseDefinitionId());
			if (target == null || !target.active()) {
				continue;
			}
			if (!ExerciseDefinitionAccessPolicy.isSelectableForPrescription(athleteId, target)) {
				continue;
			}
			if (!EquipmentCompatibilityEvaluator.isCompatible(
					target.metadata().requiredEquipment(), availableEquipment)) {
				continue;
			}
			compatible.add(new RankedSuggestionCandidate(relationship, target));
		}
		return new SuggestionOutcome(relationships.size(), compatible);
	}

	private static List<ExerciseSubstitutionSuggestionResult> rankSuggestions(
			List<RankedSuggestionCandidate> compatible,
			ExerciseDifficulty sourceDifficulty,
			int limit) {
		if (compatible.isEmpty() || limit <= 0) {
			return List.of();
		}
		List<ExerciseSubstitutionSuggestionRanker.RankableSuggestion> rankable = compatible.stream()
				.map(RankedSuggestionCandidate::toRankable)
				.toList();
		List<ExerciseSubstitutionSuggestionRanker.RankableSuggestion> ranked =
				ExerciseSubstitutionSuggestionRanker.rank(rankable, sourceDifficulty, limit);
		List<ExerciseSubstitutionSuggestionResult> results = new ArrayList<>(ranked.size());
		for (int index = 0; index < ranked.size(); index++) {
			RankedSuggestionCandidate candidate = findCandidate(compatible, ranked.get(index));
			results.add(candidate.toResult(index + 1, sourceDifficulty));
		}
		return List.copyOf(results);
	}

	private static RankedSuggestionCandidate findCandidate(
			List<RankedSuggestionCandidate> compatible,
			ExerciseSubstitutionSuggestionRanker.RankableSuggestion ranked) {
		for (RankedSuggestionCandidate candidate : compatible) {
			if (candidate.target.id().equals(ranked.targetExerciseDefinitionId())) {
				return candidate;
			}
		}
		throw new IllegalStateException("Ranked suggestion missing candidate metadata");
	}

	private static FeasibilityEnvironmentContextResult fromEnvironment(
			TrainingEnvironment environment,
			FeasibilityEnvironmentContextSource source,
			Instant snapshotCapturedAt) {
		return new FeasibilityEnvironmentContextResult(
				environment.id(),
				environment.name(),
				environment.availableEquipment(),
				source,
				snapshotCapturedAt);
	}

	private static FeasibilityEnvironmentContextResult fromSnapshot(
			WorkoutOccurrenceEnvironmentSnapshot snapshot,
			FeasibilityEnvironmentContextSource source,
			Instant snapshotCapturedAt) {
		return new FeasibilityEnvironmentContextResult(
				snapshot.trainingEnvironmentId(),
				snapshot.nameSnapshot(),
				snapshot.availableEquipmentSnapshot(),
				source,
				snapshotCapturedAt);
	}

	record PrescriptionAnalysis(
			boolean feasible,
			ExerciseCompatibilityDetailResult compatibility,
			ExerciseFeasibilityStatus currentStatus,
			FeasibilityReasonCode reasonCode,
			String reasonSummary,
			int compatibleSubstitutionCount,
			List<ExerciseSubstitutionSuggestionResult> suggestedSubstitutions,
			boolean hasCompatibleSubstitution) {

		static PrescriptionAnalysis notAnalyzable(String exerciseName) {
			return new PrescriptionAnalysis(
					false,
					new ExerciseCompatibilityDetailResult(false, List.of(), List.of(), List.of()),
					ExerciseFeasibilityStatus.NOT_ANALYZABLE,
					FeasibilityReasonCode.EXERCISE_DEFINITION_NOT_ACCESSIBLE,
					summarizeReason(FeasibilityReasonCode.EXERCISE_DEFINITION_NOT_ACCESSIBLE, List.of()),
					0,
					List.of(),
					false);
		}

		static PrescriptionAnalysis notAccessible(String exerciseName) {
			return notAnalyzable(exerciseName);
		}

		static PrescriptionAnalysis noEnvironmentContext(ExerciseDefinition definition, String exerciseName) {
			ExerciseCompatibilityDetailResult compatibility = ExerciseCompatibilityDetailResult.from(
					ExerciseEnvironmentCompatibilityEvaluator.evaluate(
							definition.metadata().requiredEquipment(), List.of()));
			return new PrescriptionAnalysis(
					false,
					compatibility,
					ExerciseFeasibilityStatus.NOT_ANALYZABLE,
					FeasibilityReasonCode.NO_ENVIRONMENT_CONTEXT,
					summarizeReason(FeasibilityReasonCode.NO_ENVIRONMENT_CONTEXT, compatibility.missingRequiredEquipment()),
					0,
					List.of(),
					false);
		}

	}

	record ExecutionAnalysis(
			ExerciseCompatibilityDetailResult prescribedCompatibility,
			ExerciseCompatibilityDetailResult performedCompatibility,
			boolean currentExecutionFeasible,
			ExerciseFeasibilityStatus currentStatus,
			FeasibilityReasonCode reasonCode,
			String reasonSummary,
			int compatibleSubstitutionCount,
			List<ExerciseSubstitutionSuggestionResult> suggestedSubstitutions,
			boolean hasCompatibleSubstitution) {

		static ExecutionAnalysis notAnalyzable(String prescribedName, String performedName, boolean substituted) {
			return new ExecutionAnalysis(
					new ExerciseCompatibilityDetailResult(false, List.of(), List.of(), List.of()),
					new ExerciseCompatibilityDetailResult(false, List.of(), List.of(), List.of()),
					false,
					ExerciseFeasibilityStatus.NOT_ANALYZABLE,
					FeasibilityReasonCode.EXERCISE_DEFINITION_NOT_ACCESSIBLE,
					summarizeReason(FeasibilityReasonCode.EXERCISE_DEFINITION_NOT_ACCESSIBLE, List.of()),
					0,
					List.of(),
					false);
		}

		static ExecutionAnalysis notAccessible(String prescribedName, String performedName, boolean substituted) {
			return notAnalyzable(prescribedName, performedName, substituted);
		}

		static ExecutionAnalysis noEnvironmentContext(
				ExerciseDefinition prescribed,
				ExerciseDefinition performed,
				String prescribedName,
				String performedName,
				boolean substituted) {
			ExerciseCompatibilityDetailResult prescribedCompatibility = ExerciseCompatibilityDetailResult.from(
					ExerciseEnvironmentCompatibilityEvaluator.evaluate(
							prescribed.metadata().requiredEquipment(), List.of()));
			ExerciseCompatibilityDetailResult performedCompatibility = ExerciseCompatibilityDetailResult.from(
					ExerciseEnvironmentCompatibilityEvaluator.evaluate(
							performed.metadata().requiredEquipment(), List.of()));
			return new ExecutionAnalysis(
					prescribedCompatibility,
					performedCompatibility,
					false,
					ExerciseFeasibilityStatus.NOT_ANALYZABLE,
					FeasibilityReasonCode.NO_ENVIRONMENT_CONTEXT,
					summarizeReason(FeasibilityReasonCode.NO_ENVIRONMENT_CONTEXT,
							performedCompatibility.missingRequiredEquipment()),
					0,
					List.of(),
					false);
		}

	}

	private record SuggestionOutcome(int totalRelationships, List<RankedSuggestionCandidate> compatible) {
	}

	private record RankedSuggestionCandidate(
			ExerciseSubstitutionRelationship relationship,
			ExerciseDefinition target) {

		ExerciseSubstitutionSuggestionRanker.RankableSuggestion toRankable() {
			return new ExerciseSubstitutionSuggestionRanker.RankableSuggestion() {
				@Override
				public com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility compatibilityLevel() {
					return relationship.compatibilityLevel();
				}

				@Override
				public com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType relationshipType() {
					return relationship.relationshipType();
				}

				@Override
				public List<EquipmentType> targetRequiredEquipment() {
					return target.metadata().requiredEquipment();
				}

				@Override
				public ExerciseDifficulty targetDifficulty() {
					return target.metadata().difficulty();
				}

				@Override
				public String targetCanonicalName() {
					return target.canonicalName();
				}

				@Override
				public ExerciseDefinitionId targetExerciseDefinitionId() {
					return target.id();
				}
			};
		}

		ExerciseSubstitutionSuggestionResult toResult(int rankingPosition, ExerciseDifficulty sourceDifficulty) {
			return new ExerciseSubstitutionSuggestionResult(
					rankingPosition,
					relationship.id(),
					target.id(),
					target.canonicalName(),
					relationship.compatibilityLevel(),
					relationship.relationshipType(),
					ExerciseSubstitutionSuggestionRanker.externalEquipmentBurden(target.metadata().requiredEquipment()),
					ExerciseSubstitutionSuggestionRanker.difficultyProximityRank(
							sourceDifficulty, target.metadata().difficulty()),
					relationship.rationale(),
					target.metadata().requiredEquipment(),
					target.metadata().difficulty());
		}

	}

}
