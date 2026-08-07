package com.devinolabs.uap.training.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Composes {@link ExerciseSubstitutionSuggestionRanker} with optional lower-impact preference.
 * Does not bypass equipment/compatibility filters — only reorders already-compatible candidates.
 */
public final class RecommendationAwareSubstitutionSuggestionRanker {

	private RecommendationAwareSubstitutionSuggestionRanker() {
	}

	public interface ImpactAwareRankable extends ExerciseSubstitutionSuggestionRanker.RankableSuggestion {
		ImpactLevel targetImpactLevel();
	}

	public static List<ExerciseSubstitutionSuggestionRanker.RankableSuggestion> rank(
			List<? extends ImpactAwareRankable> suggestions,
			ExerciseDifficulty sourceDifficulty,
			ImpactLevel sourceImpactLevel,
			boolean preferLowerImpact,
			int limit) {
		Objects.requireNonNull(suggestions, "suggestions must not be null");
		if (limit < 0) {
			throw new IllegalArgumentException("limit must not be negative");
		}
		if (!preferLowerImpact || sourceImpactLevel == null) {
			return ExerciseSubstitutionSuggestionRanker.rank(suggestions, sourceDifficulty, limit);
		}
		Comparator<ImpactAwareRankable> comparator = Comparator
				.comparingInt((ImpactAwareRankable suggestion) -> ImpactLevelOrdering.relativePreferenceGroup(
						sourceImpactLevel, suggestion.targetImpactLevel()))
				.thenComparing(ExerciseSubstitutionSuggestionRanker.comparator(sourceDifficulty));
		return suggestions.stream()
				.sorted(comparator)
				.limit(limit)
				.map(ExerciseSubstitutionSuggestionRanker.RankableSuggestion.class::cast)
				.toList();
	}

}
