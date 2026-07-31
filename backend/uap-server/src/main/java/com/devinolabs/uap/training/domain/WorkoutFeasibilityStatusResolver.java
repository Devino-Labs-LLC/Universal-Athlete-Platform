package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Derives workout-level feasibility status and percentage from exercise-level outcomes.
 */
public final class WorkoutFeasibilityStatusResolver {

	private WorkoutFeasibilityStatusResolver() {
	}

	public static WorkoutFeasibilityStatus resolve(
			int totalExercises,
			int feasibleExercises,
			boolean environmentContextPresent) {
		if (totalExercises <= 0) {
			return WorkoutFeasibilityStatus.NO_EXERCISES;
		}
		if (!environmentContextPresent) {
			return WorkoutFeasibilityStatus.NO_ENVIRONMENT_CONTEXT;
		}
		if (feasibleExercises == totalExercises) {
			return WorkoutFeasibilityStatus.FULLY_FEASIBLE;
		}
		if (feasibleExercises == 0) {
			return WorkoutFeasibilityStatus.NOT_FEASIBLE;
		}
		return WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE;
	}

	/**
	 * Plan aggregation when some days may lack environment context.
	 *
	 * <p>Days without context contribute their exercises to {@code exercisesWithoutContext}. Those
	 * exercises are never counted as feasible. Mixed context or mixed feasibility yields
	 * {@link WorkoutFeasibilityStatus#PARTIALLY_FEASIBLE}.
	 */
	public static WorkoutFeasibilityStatus resolvePlan(
			int totalExercises,
			int feasibleExercises,
			int exercisesWithoutContext,
			int analyzableExercises) {
		if (totalExercises <= 0) {
			return WorkoutFeasibilityStatus.NO_EXERCISES;
		}
		if (analyzableExercises <= 0) {
			return WorkoutFeasibilityStatus.NO_ENVIRONMENT_CONTEXT;
		}
		if (exercisesWithoutContext > 0) {
			if (feasibleExercises > 0 && feasibleExercises < analyzableExercises) {
				return WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE;
			}
			if (feasibleExercises == analyzableExercises && analyzableExercises > 0) {
				return WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE;
			}
			if (feasibleExercises == 0) {
				return WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE;
			}
			return WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE;
		}
		return resolve(analyzableExercises, feasibleExercises, true);
	}

	public static BigDecimal percentage(int totalExercises, int feasibleExercises) {
		if (totalExercises <= 0) {
			return null;
		}
		return BigDecimal.valueOf(feasibleExercises)
				.multiply(BigDecimal.valueOf(100))
				.divide(BigDecimal.valueOf(totalExercises), 2, RoundingMode.HALF_UP);
	}

	public static WorkoutFeasibilitySummary summarize(
			int totalExercises,
			int feasibleExercises,
			int substitutedExecutions,
			int feasibleAfterExistingSubstitution,
			int exercisesWithCompatibleSuggestions,
			boolean environmentContextPresent) {
		int infeasible = Math.max(0, totalExercises - feasibleExercises);
		int withoutSuggestions = Math.max(0, infeasible - exercisesWithCompatibleSuggestions);
		WorkoutFeasibilityStatus status = resolve(totalExercises, feasibleExercises, environmentContextPresent);
		return new WorkoutFeasibilitySummary(
				status,
				totalExercises,
				feasibleExercises,
				infeasible,
				substitutedExecutions,
				feasibleAfterExistingSubstitution,
				exercisesWithCompatibleSuggestions,
				withoutSuggestions,
				percentage(totalExercises, feasibleExercises),
				environmentContextPresent);
	}

	public record WorkoutFeasibilitySummary(
			WorkoutFeasibilityStatus status,
			int totalExercises,
			int feasibleExercises,
			int infeasibleExercises,
			int substitutedExecutions,
			int feasibleAfterExistingSubstitution,
			int exercisesWithCompatibleSuggestions,
			int exercisesWithoutCompatibleSuggestions,
			BigDecimal feasibilityPercentage,
			boolean environmentContextPresent) {

		public WorkoutFeasibilitySummary {
			Objects.requireNonNull(status, "status must not be null");
		}

	}

}
