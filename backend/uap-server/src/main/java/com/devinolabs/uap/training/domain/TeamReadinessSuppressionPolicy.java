package com.devinolabs.uap.training.domain;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Deterministic cell suppression for Team Readiness aggregates.
 *
 * <p>This is ordinary complementary cell suppression, not differential privacy.
 * Counts below {@value #MIN_COHORT_SIZE} are not published. When any cell is
 * suppressed, the exact cohort total is also withheld so a published cell cannot
 * be subtracted from the total to recover a hidden count. If any positive-count
 * cell is primary-suppressed, the smallest remaining exact cell is suppressed as
 * well so a single leftover published cell cannot complete the partition.
 */
public final class TeamReadinessSuppressionPolicy {

	public static final int MIN_COHORT_SIZE = 5;

	private TeamReadinessSuppressionPolicy() {
	}

	public static SuppressedDistribution suppress(List<NamedCount> cells) {
		Objects.requireNonNull(cells, "cells must not be null");
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (NamedCount cell : cells) {
			Objects.requireNonNull(cell, "cell must not be null");
			if (cell.count() < 0) {
				throw new IllegalArgumentException("cell count must not be negative");
			}
			counts.put(cell.name(), cell.count());
		}
		int total = counts.values().stream().mapToInt(Integer::intValue).sum();
		return suppress(total, counts);
	}

	/**
	 * Non-partition metrics such as limiting dimensions. {@code eligibleCohort} is
	 * the number of athletes who may contribute, not the sum of overlapping cells.
	 */
	public static SuppressedDistribution suppressEligible(int eligibleCohort, List<NamedCount> cells) {
		Objects.requireNonNull(cells, "cells must not be null");
		if (eligibleCohort < 0) {
			throw new IllegalArgumentException("eligible cohort must not be negative");
		}
		Map<String, Integer> counts = new LinkedHashMap<>();
		for (NamedCount cell : cells) {
			Objects.requireNonNull(cell, "cell must not be null");
			if (cell.count() < 0) {
				throw new IllegalArgumentException("cell count must not be negative");
			}
			counts.put(cell.name(), cell.count());
		}
		return suppress(eligibleCohort, counts);
	}

	private static SuppressedDistribution suppress(int total, Map<String, Integer> counts) {
		if (total < MIN_COHORT_SIZE) {
			return new SuppressedDistribution(
					AggregateStatus.INSUFFICIENT_DATA,
					CohortPublication.BELOW_MINIMUM,
					null,
					List.of());
		}

		List<String> primarySuppressed = counts.entrySet().stream()
				.filter(entry -> entry.getValue() < MIN_COHORT_SIZE)
				.map(Map.Entry::getKey)
				.toList();
		boolean complementaryRequired = primarySuppressed.stream()
				.anyMatch(name -> counts.get(name) > 0);
		String complementaryName = null;
		if (complementaryRequired) {
			complementaryName = counts.entrySet().stream()
					.filter(entry -> entry.getValue() >= MIN_COHORT_SIZE)
					.min(Comparator.<Map.Entry<String, Integer>>comparingInt(Map.Entry::getValue)
							.thenComparing(Map.Entry::getKey))
					.map(Map.Entry::getKey)
					.orElse(null);
		}

		List<SuppressedCell> publishedCells = new ArrayList<>();
		boolean anySuppressed = !primarySuppressed.isEmpty() || complementaryName != null;
		for (Map.Entry<String, Integer> entry : counts.entrySet()) {
			boolean suppressed = primarySuppressed.contains(entry.getKey())
					|| entry.getKey().equals(complementaryName);
			if (suppressed) {
				publishedCells.add(new SuppressedCell(entry.getKey(), CellPublication.SUPPRESSED, null));
			}
			else {
				publishedCells.add(new SuppressedCell(entry.getKey(), CellPublication.PUBLISHED, entry.getValue()));
			}
		}
		return new SuppressedDistribution(
				AggregateStatus.PUBLISHED,
				anySuppressed ? CohortPublication.AT_LEAST_MINIMUM : CohortPublication.EXACT,
				anySuppressed ? null : total,
				List.copyOf(publishedCells));
	}

	public record NamedCount(String name, int count) {
		public NamedCount {
			Objects.requireNonNull(name, "name must not be null");
		}
	}

	public record SuppressedCell(String name, CellPublication publication, Integer count) {
		public SuppressedCell {
			Objects.requireNonNull(name, "name must not be null");
			Objects.requireNonNull(publication, "publication must not be null");
			if (publication == CellPublication.PUBLISHED && count == null) {
				throw new IllegalArgumentException("published cell requires a count");
			}
			if (publication == CellPublication.SUPPRESSED && count != null) {
				throw new IllegalArgumentException("suppressed cell must not carry a count");
			}
		}
	}

	public record SuppressedDistribution(
			AggregateStatus status,
			CohortPublication cohort,
			Integer includedCount,
			List<SuppressedCell> cells) {
		public SuppressedDistribution {
			Objects.requireNonNull(status, "status must not be null");
			Objects.requireNonNull(cohort, "cohort must not be null");
			cells = List.copyOf(cells);
			if (status == AggregateStatus.INSUFFICIENT_DATA && includedCount != null) {
				throw new IllegalArgumentException("insufficient data must not publish a count");
			}
			if (cohort != CohortPublication.EXACT && includedCount != null) {
				throw new IllegalArgumentException("non-exact cohort must not publish a count");
			}
		}
	}

	public enum AggregateStatus {
		INSUFFICIENT_DATA,
		PUBLISHED
	}

	public enum CohortPublication {
		BELOW_MINIMUM,
		AT_LEAST_MINIMUM,
		EXACT
	}

	public enum CellPublication {
		PUBLISHED,
		SUPPRESSED
	}

}
