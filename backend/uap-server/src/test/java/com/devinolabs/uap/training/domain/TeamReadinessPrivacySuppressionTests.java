package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.AggregateStatus;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.CellPublication;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.CohortPublication;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.NamedCount;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.SuppressedCell;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.SuppressedDistribution;

class TeamReadinessPrivacySuppressionTests {

	@Test
	void cohortBelowFiveDoesNotPublishExactCounts() {
		assertInsufficient(0);
		assertInsufficient(1);
		assertInsufficient(4);
	}

	@Test
	void fiveAthletesWithFourAndOneDoesNotPublishSmallCells() {
		SuppressedDistribution result = TeamReadinessSuppressionPolicy.suppress(List.of(
				new NamedCount("HIGH", 4),
				new NamedCount("LOW", 1)));

		assertThat(result.status()).isEqualTo(AggregateStatus.PUBLISHED);
		assertThat(result.cohort()).isEqualTo(CohortPublication.AT_LEAST_MINIMUM);
		assertThat(result.includedCount()).isNull();
		assertThat(result.cells()).allSatisfy(cell -> {
			assertThat(cell.publication()).isEqualTo(CellPublication.SUPPRESSED);
			assertThat(cell.count()).isNull();
		});
	}

	@Test
	void caseADoesNotPublishTotalTenAndExactSevenWhileMaskingThree() {
		SuppressedDistribution result = TeamReadinessSuppressionPolicy.suppress(List.of(
				new NamedCount("HIGH", 7),
				new NamedCount("LOW", 3)));

		assertThat(result.includedCount()).isNull();
		assertThat(publishedCount(result, "HIGH")).isNull();
		assertThat(publishedCount(result, "LOW")).isNull();
		assertThat(result.cells()).noneMatch(cell -> Integer.valueOf(7).equals(cell.count()));
		assertThat(result.cells()).noneMatch(cell -> Integer.valueOf(3).equals(cell.count()));
	}

	@Test
	void caseBDoesNotAllowRemainderRecoveryOfThree() {
		SuppressedDistribution result = TeamReadinessSuppressionPolicy.suppress(List.of(
				new NamedCount("HIGH", 12),
				new NamedCount("MODERATE", 5),
				new NamedCount("LOW", 3)));

		assertThat(result.includedCount()).isNull();
		assertThat(publishedCount(result, "LOW")).isNull();
		assertThat(publishedCount(result, "MODERATE")).isNull();
		assertThat(publishedCount(result, "HIGH")).isEqualTo(12);
		Integer recovered = recoverSingleSuppressed(result.includedCount(), result.cells());
		assertThat(recovered).isNull();
	}

	@Test
	void severalSmallCellsAreNotExposedByRemainder() {
		SuppressedDistribution result = TeamReadinessSuppressionPolicy.suppress(List.of(
				new NamedCount("HIGH", 6),
				new NamedCount("MODERATE", 3),
				new NamedCount("LOW", 2),
				new NamedCount("INSUFFICIENT_DATA", 1)));

		assertThat(result.includedCount()).isNull();
		assertThat(result.cells()).allSatisfy(cell -> assertThat(cell.count()).isNull());
		assertThat(recoverSingleSuppressed(result.includedCount(), result.cells())).isNull();
	}

	@Test
	void exactTotalIsPublishedOnlyWhenEveryCellIsSafe() {
		SuppressedDistribution result = TeamReadinessSuppressionPolicy.suppress(List.of(
				new NamedCount("HIGH", 6),
				new NamedCount("MODERATE", 5),
				new NamedCount("LOW", 5),
				new NamedCount("INSUFFICIENT_DATA", 5)));

		assertThat(result.cohort()).isEqualTo(CohortPublication.EXACT);
		assertThat(result.includedCount()).isEqualTo(21);
		assertThat(publishedCount(result, "HIGH")).isEqualTo(6);
	}

	@Test
	void complementarySuppressionBreaksEqualCountTiesByName() {
		SuppressedDistribution result = TeamReadinessSuppressionPolicy.suppress(List.of(
				new NamedCount("HIGH", 8),
				new NamedCount("LOW", 5),
				new NamedCount("MODERATE", 5),
				new NamedCount("INSUFFICIENT_DATA", 2)));

		assertThat(result.includedCount()).isNull();
		assertThat(publishedCount(result, "INSUFFICIENT_DATA")).isNull();
		assertThat(publishedCount(result, "LOW")).isNull();
		assertThat(publishedCount(result, "MODERATE")).isEqualTo(5);
		assertThat(publishedCount(result, "HIGH")).isEqualTo(8);
	}

	@Test
	void rejectsNegativeCountsAndNegativeEligibleCohort() {
		assertThatThrownBy(() -> TeamReadinessSuppressionPolicy.suppress(List.of(new NamedCount("HIGH", -1))))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> TeamReadinessSuppressionPolicy.suppressEligible(-1, List.of()))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new NamedCount(null, 1))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void dimensionCohortUsesEligibleAthletesNotOverlappingSum() {
		SuppressedDistribution result = TeamReadinessSuppressionPolicy.suppressEligible(
				3,
				List.of(new NamedCount("FATIGUE", 3), new NamedCount("STRESS", 3)));

		assertThat(result.status()).isEqualTo(AggregateStatus.INSUFFICIENT_DATA);
		assertThat(result.includedCount()).isNull();
		assertThat(result.cells()).isEmpty();
	}

	private static void assertInsufficient(int high) {
		SuppressedDistribution result = TeamReadinessSuppressionPolicy.suppress(List.of(
				new NamedCount("HIGH", high)));
		assertThat(result.status()).isEqualTo(AggregateStatus.INSUFFICIENT_DATA);
		assertThat(result.cohort()).isEqualTo(CohortPublication.BELOW_MINIMUM);
		assertThat(result.includedCount()).isNull();
		assertThat(result.cells()).isEmpty();
	}

	private static Integer publishedCount(SuppressedDistribution result, String name) {
		return result.cells().stream()
				.filter(cell -> cell.name().equals(name))
				.findFirst()
				.map(SuppressedCell::count)
				.orElse(null);
	}

	private static Integer recoverSingleSuppressed(Integer total, List<SuppressedCell> cells) {
		if (total == null) {
			return null;
		}
		long suppressed = cells.stream().filter(cell -> cell.publication() == CellPublication.SUPPRESSED).count();
		if (suppressed != 1) {
			return null;
		}
		int published = cells.stream()
				.filter(cell -> cell.count() != null)
				.mapToInt(SuppressedCell::count)
				.sum();
		return total - published;
	}

}
