package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class RecommendationAwareSubstitutionSuggestionRankerTests {

	@Test
	void prefersLowerImpactThenSameThenHigherWhilePreservingBaseOrderWithinGroup() {
		Rankable low = candidate("LOW", ImpactLevel.LOW_IMPACT, ExerciseSubstitutionCompatibility.HIGH, List.of());
		Rankable moderate = candidate(
				"MOD", ImpactLevel.MODERATE_IMPACT, ExerciseSubstitutionCompatibility.HIGH, List.of());
		Rankable high = candidate("HIGH", ImpactLevel.HIGH_IMPACT, ExerciseSubstitutionCompatibility.HIGH, List.of());

		List<ExerciseSubstitutionSuggestionRanker.RankableSuggestion> ranked =
				RecommendationAwareSubstitutionSuggestionRanker.rank(
						List.of(high, moderate, low),
						ExerciseDifficulty.INTERMEDIATE,
						ImpactLevel.HIGH_IMPACT,
						true,
						10);

		assertThat(ranked).extracting(ExerciseSubstitutionSuggestionRanker.RankableSuggestion::targetCanonicalName)
				.containsExactly("LOW", "MOD", "HIGH");
	}

	@Test
	void withoutLowerImpactPreferenceUsesExistingRankerOrder() {
		Rankable lowBurden = candidate(
				"A", ImpactLevel.HIGH_IMPACT, ExerciseSubstitutionCompatibility.HIGH, List.of());
		Rankable highBurden = candidate(
				"B",
				ImpactLevel.LOW_IMPACT,
				ExerciseSubstitutionCompatibility.HIGH,
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH));

		List<ExerciseSubstitutionSuggestionRanker.RankableSuggestion> ranked =
				RecommendationAwareSubstitutionSuggestionRanker.rank(
						List.of(highBurden, lowBurden),
						ExerciseDifficulty.INTERMEDIATE,
						ImpactLevel.HIGH_IMPACT,
						false,
						10);

		assertThat(ranked).extracting(ExerciseSubstitutionSuggestionRanker.RankableSuggestion::targetCanonicalName)
				.containsExactly("A", "B");
	}

	@Test
	void whenNoLowerImpactCandidateExistingOrderRemainsValid() {
		Rankable first = candidate(
				"FIRST", ImpactLevel.HIGH_IMPACT, ExerciseSubstitutionCompatibility.HIGH, List.of());
		Rankable second = candidate(
				"SECOND", ImpactLevel.HIGH_IMPACT, ExerciseSubstitutionCompatibility.MODERATE, List.of());

		List<ExerciseSubstitutionSuggestionRanker.RankableSuggestion> ranked =
				RecommendationAwareSubstitutionSuggestionRanker.rank(
						List.of(second, first),
						ExerciseDifficulty.INTERMEDIATE,
						ImpactLevel.HIGH_IMPACT,
						true,
						10);

		assertThat(ranked).extracting(ExerciseSubstitutionSuggestionRanker.RankableSuggestion::targetCanonicalName)
				.containsExactly("FIRST", "SECOND");
	}

	private static Rankable candidate(
			String name,
			ImpactLevel impact,
			ExerciseSubstitutionCompatibility compatibility,
			List<EquipmentType> equipment) {
		return new Rankable(
				ExerciseDefinitionId.of(UUID.randomUUID()),
				name,
				compatibility,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				equipment,
				ExerciseDifficulty.INTERMEDIATE,
				impact);
	}

	private record Rankable(
			ExerciseDefinitionId targetExerciseDefinitionId,
			String targetCanonicalName,
			ExerciseSubstitutionCompatibility compatibilityLevel,
			ExerciseSubstitutionRelationshipType relationshipType,
			List<EquipmentType> targetRequiredEquipment,
			ExerciseDifficulty targetDifficulty,
			ImpactLevel targetImpactLevel)
			implements RecommendationAwareSubstitutionSuggestionRanker.ImpactAwareRankable {
	}

}
