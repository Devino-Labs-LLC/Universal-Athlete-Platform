package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class WorkoutFeasibilityStatusResolverTests {

	@Test
	void resolveReturnsNoExercisesWhenEmpty() {
		assertThat(WorkoutFeasibilityStatusResolver.resolve(0, 0, true))
				.isEqualTo(WorkoutFeasibilityStatus.NO_EXERCISES);
		assertThat(WorkoutFeasibilityStatusResolver.percentage(0, 0)).isNull();
	}

	@Test
	void resolveReturnsNoEnvironmentContextWhenContextMissing() {
		assertThat(WorkoutFeasibilityStatusResolver.resolve(4, 2, false))
				.isEqualTo(WorkoutFeasibilityStatus.NO_ENVIRONMENT_CONTEXT);
	}

	@Test
	void resolveReturnsFullyPartiallyAndNotFeasible() {
		assertThat(WorkoutFeasibilityStatusResolver.resolve(4, 4, true))
				.isEqualTo(WorkoutFeasibilityStatus.FULLY_FEASIBLE);
		assertThat(WorkoutFeasibilityStatusResolver.resolve(4, 2, true))
				.isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);
		assertThat(WorkoutFeasibilityStatusResolver.resolve(4, 0, true))
				.isEqualTo(WorkoutFeasibilityStatus.NOT_FEASIBLE);
	}

	@Test
	void percentageUsesHalfUpScaleTwo() {
		assertThat(WorkoutFeasibilityStatusResolver.percentage(4, 2))
				.isEqualByComparingTo(new BigDecimal("50.00"));
		assertThat(WorkoutFeasibilityStatusResolver.percentage(3, 1))
				.isEqualByComparingTo(new BigDecimal("33.33"));
	}

	@Test
	void resolvePlanHandlesMixedContextAsPartiallyFeasible() {
		assertThat(WorkoutFeasibilityStatusResolver.resolvePlan(4, 2, 2, 2))
				.isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);
		assertThat(WorkoutFeasibilityStatusResolver.resolvePlan(4, 4, 0, 4))
				.isEqualTo(WorkoutFeasibilityStatus.FULLY_FEASIBLE);
		assertThat(WorkoutFeasibilityStatusResolver.resolvePlan(4, 0, 4, 0))
				.isEqualTo(WorkoutFeasibilityStatus.NO_ENVIRONMENT_CONTEXT);
	}

	@Test
	void summarizeComputesDerivedCounts() {
		var summary = WorkoutFeasibilityStatusResolver.summarize(4, 2, 1, 1, 2, true);
		assertThat(summary.status()).isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);
		assertThat(summary.infeasibleExercises()).isEqualTo(2);
		assertThat(summary.exercisesWithoutCompatibleSuggestions()).isZero();
		assertThat(summary.feasibilityPercentage()).isEqualByComparingTo(new BigDecimal("50.00"));
	}

}
