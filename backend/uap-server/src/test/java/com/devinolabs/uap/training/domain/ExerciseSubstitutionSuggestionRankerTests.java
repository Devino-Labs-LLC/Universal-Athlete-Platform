package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.training.application.ExerciseSubstitutionSuggestionResult;

class ExerciseSubstitutionSuggestionRankerTests {

	@Test
	void ranksHighBeforeModerateAndEquipmentAlternativesFirst() {
		ExerciseSubstitutionSuggestionResult high = suggestion(
				SystemExerciseDefinitions.GOBLET_SQUAT,
				"Goblet Squat",
				ExerciseSubstitutionCompatibility.HIGH,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				List.of(EquipmentType.DUMBBELL),
				ExerciseDifficulty.BEGINNER);
		ExerciseSubstitutionSuggestionResult moderate = suggestion(
				SystemExerciseDefinitions.LEG_PRESS,
				"Leg Press",
				ExerciseSubstitutionCompatibility.MODERATE,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				List.of(EquipmentType.PLATE_LOADED_MACHINE),
				ExerciseDifficulty.BEGINNER);
		List<ExerciseSubstitutionSuggestionRanker.RankableSuggestion> ranked =
				ExerciseSubstitutionSuggestionRanker.rank(
						List.of(moderate, high),
						ExerciseDifficulty.INTERMEDIATE,
						3);
		assertThat(ranked).extracting(ExerciseSubstitutionSuggestionRanker.RankableSuggestion::targetCanonicalName)
				.containsExactly("Goblet Squat", "Leg Press");
	}

	@Test
	void respectsLimitIncludingZero() {
		ExerciseSubstitutionSuggestionResult high = suggestion(
				SystemExerciseDefinitions.GOBLET_SQUAT,
				"Goblet Squat",
				ExerciseSubstitutionCompatibility.HIGH,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				List.of(EquipmentType.DUMBBELL),
				ExerciseDifficulty.BEGINNER);
		assertThat(ExerciseSubstitutionSuggestionRanker.rank(
				List.of(high), ExerciseDifficulty.INTERMEDIATE, 0)).isEmpty();
		assertThat(ExerciseSubstitutionSuggestionRanker.rank(
				List.of(high), ExerciseDifficulty.INTERMEDIATE, 1)).hasSize(1);
	}

	@Test
	void rejectsNegativeLimit() {
		assertThatThrownBy(() -> ExerciseSubstitutionSuggestionRanker.rank(List.of(), ExerciseDifficulty.BEGINNER, -1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private static ExerciseSubstitutionSuggestionResult suggestion(
			ExerciseDefinitionId id,
			String name,
			ExerciseSubstitutionCompatibility compatibility,
			ExerciseSubstitutionRelationshipType type,
			List<EquipmentType> equipment,
			ExerciseDifficulty difficulty) {
		return new ExerciseSubstitutionSuggestionResult(
				0,
				ExerciseSubstitutionRelationshipId.generate(),
				id,
				name,
				compatibility,
				type,
				ExerciseSubstitutionSuggestionRanker.externalEquipmentBurden(equipment),
				ExerciseSubstitutionSuggestionRanker.difficultyProximityRank(
						ExerciseDifficulty.INTERMEDIATE, difficulty),
				"rationale",
				equipment,
				difficulty,
				com.devinolabs.uap.training.domain.ImpactLevel.MODERATE_IMPACT);
	}

}
