package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.AggregateStatus;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.CellPublication;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.CohortPublication;

/**
 * Allow-list Team Readiness projection. No athlete identity, scores, or consent identifiers.
 */
public record TeamReadinessResult(
		UUID teamId,
		LocalDate date,
		AggregateStatus status,
		CohortPublication cohort,
		Integer includedCount,
		CategoryDistribution categoryDistribution,
		DimensionDistribution limitingDimensionDistribution,
		AvailabilityAggregate availability) {

	public record CategoryDistribution(
			AggregateStatus status,
			CohortPublication cohort,
			Integer includedCount,
			List<CategoryCell> cells) {
	}

	public record CategoryCell(String category, CellPublication publication, Integer count) {
	}

	public record DimensionDistribution(
			AggregateStatus status,
			CohortPublication cohort,
			Integer includedCount,
			List<DimensionCell> cells) {
	}

	public record DimensionCell(String dimension, CellPublication publication, Integer count) {
	}

	public record AvailabilityAggregate(String status) {
	}

}
