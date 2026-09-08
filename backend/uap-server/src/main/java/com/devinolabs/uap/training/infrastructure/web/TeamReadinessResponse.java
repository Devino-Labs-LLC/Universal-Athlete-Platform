package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.devinolabs.uap.training.application.TeamReadinessResult;

@JsonInclude(JsonInclude.Include.NON_NULL)
record TeamReadinessResponse(
		UUID teamId,
		LocalDate date,
		String status,
		String cohort,
		Integer includedCount,
		CategoryDistribution categoryDistribution,
		DimensionDistribution limitingDimensionDistribution,
		AvailabilityAggregate availability) {

	static TeamReadinessResponse from(TeamReadinessResult result) {
		return new TeamReadinessResponse(
				result.teamId(),
				result.date(),
				result.status().name(),
				result.cohort().name(),
				result.includedCount(),
				new CategoryDistribution(
						result.categoryDistribution().status().name(),
						result.categoryDistribution().cohort().name(),
						result.categoryDistribution().includedCount(),
						result.categoryDistribution().cells().stream()
								.map(cell -> new CategoryCell(cell.category(), cell.publication().name(), cell.count()))
								.toList()),
				new DimensionDistribution(
						result.limitingDimensionDistribution().status().name(),
						result.limitingDimensionDistribution().cohort().name(),
						result.limitingDimensionDistribution().includedCount(),
						result.limitingDimensionDistribution().cells().stream()
								.map(cell -> new DimensionCell(cell.dimension(), cell.publication().name(), cell.count()))
								.toList()),
				new AvailabilityAggregate(result.availability().status()));
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record CategoryDistribution(
			String status,
			String cohort,
			Integer includedCount,
			List<CategoryCell> cells) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record CategoryCell(String category, String publication, Integer count) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record DimensionDistribution(
			String status,
			String cohort,
			Integer includedCount,
			List<DimensionCell> cells) {
	}

	@JsonInclude(JsonInclude.Include.NON_NULL)
	record DimensionCell(String dimension, String publication, Integer count) {
	}

	record AvailabilityAggregate(String status) {
	}

}
