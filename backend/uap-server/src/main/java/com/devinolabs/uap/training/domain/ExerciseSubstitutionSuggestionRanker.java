package com.devinolabs.uap.training.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic ranking for environment-compatible substitution suggestions.
 *
 * <p>Order:
 * <ol>
 *   <li>Compatibility: HIGH, MODERATE, CONDITIONAL</li>
 *   <li>Relationship type priority (equipment alternatives before progressions, etc.)</li>
 *   <li>Fewer externally required equipment items</li>
 *   <li>Difficulty proximity to the source (same, then one level lower, then one higher)</li>
 *   <li>Target canonical name ascending</li>
 *   <li>Target definition ID</li>
 * </ol>
 */
public final class ExerciseSubstitutionSuggestionRanker {

	private ExerciseSubstitutionSuggestionRanker() {
	}

	public static Comparator<RankableSuggestion> comparator(ExerciseDifficulty sourceDifficulty) {
		ExerciseDifficulty source = sourceDifficulty;
		return Comparator
				.comparingInt((RankableSuggestion suggestion) -> compatibilityRank(suggestion.compatibilityLevel()))
				.thenComparingInt(suggestion -> relationshipTypeRank(suggestion.relationshipType()))
				.thenComparingInt(suggestion -> externalEquipmentBurden(suggestion.targetRequiredEquipment()))
				.thenComparingInt(suggestion -> difficultyProximityRank(source, suggestion.targetDifficulty()))
				.thenComparing(RankableSuggestion::targetCanonicalName, String.CASE_INSENSITIVE_ORDER)
				.thenComparing(suggestion -> suggestion.targetExerciseDefinitionId().value());
	}

	public static List<RankableSuggestion> rank(
			List<? extends RankableSuggestion> suggestions,
			ExerciseDifficulty sourceDifficulty,
			int limit) {
		Objects.requireNonNull(suggestions, "suggestions must not be null");
		if (limit < 0) {
			throw new IllegalArgumentException("limit must not be negative");
		}
		return suggestions.stream()
				.sorted(comparator(sourceDifficulty))
				.limit(limit)
				.map(RankableSuggestion.class::cast)
				.toList();
	}

	public static int compatibilityRank(ExerciseSubstitutionCompatibility compatibility) {
		return switch (Objects.requireNonNull(compatibility, "compatibility must not be null")) {
			case HIGH -> 0;
			case MODERATE -> 1;
			case CONDITIONAL -> 2;
		};
	}

	public static int relationshipTypeRank(ExerciseSubstitutionRelationshipType type) {
		return switch (Objects.requireNonNull(type, "type must not be null")) {
			case EQUIVALENT_VARIATION -> 0;
			case EQUIPMENT_ALTERNATIVE -> 1;
			case REGRESSION -> 2;
			case LOWER_IMPACT_ALTERNATIVE -> 3;
			case UNILATERAL_ALTERNATIVE -> 4;
			case BILATERAL_ALTERNATIVE -> 5;
			case TEMPORARY_MODIFICATION -> 6;
			case SPORT_SPECIFIC_VARIATION -> 7;
			case PROGRESSION -> 8;
			case OTHER -> 9;
		};
	}

	public static int externalEquipmentBurden(List<EquipmentType> requiredEquipment) {
		if (requiredEquipment == null || requiredEquipment.isEmpty()) {
			return 0;
		}
		int count = 0;
		for (EquipmentType type : requiredEquipment) {
			if (type != null && type != EquipmentType.BODYWEIGHT) {
				count++;
			}
		}
		return count;
	}

	public static int difficultyProximityRank(ExerciseDifficulty source, ExerciseDifficulty target) {
		if (source == null || target == null) {
			return 100;
		}
		int delta = target.ordinal() - source.ordinal();
		if (delta == 0) {
			return 0;
		}
		if (delta == -1) {
			return 1;
		}
		if (delta == 1) {
			return 2;
		}
		return 3 + Math.abs(delta);
	}

	/**
	 * Minimal projection used by the ranker. Application-layer suggestion records implement this.
	 */
	public interface RankableSuggestion {

		ExerciseSubstitutionCompatibility compatibilityLevel();

		ExerciseSubstitutionRelationshipType relationshipType();

		List<EquipmentType> targetRequiredEquipment();

		ExerciseDifficulty targetDifficulty();

		String targetCanonicalName();

		ExerciseDefinitionId targetExerciseDefinitionId();

	}

}
