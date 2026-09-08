package com.devinolabs.uap.training.infrastructure.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.training.application.TeamReadinessResult;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.AggregateStatus;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.CellPublication;
import com.devinolabs.uap.training.domain.TeamReadinessSuppressionPolicy.CohortPublication;

class TeamReadinessResponseMappingTests {

	@Test
	void mapsAllowListFieldsAndOmitsAthleteIdentity() {
		UUID teamId = UUID.fromString("10000000-0000-0000-0000-000000000001");
		TeamReadinessResult result = new TeamReadinessResult(
				teamId,
				LocalDate.of(2026, 9, 7),
				AggregateStatus.PUBLISHED,
				CohortPublication.AT_LEAST_MINIMUM,
				null,
				new TeamReadinessResult.CategoryDistribution(
						AggregateStatus.PUBLISHED,
						CohortPublication.AT_LEAST_MINIMUM,
						null,
						List.of(new TeamReadinessResult.CategoryCell("HIGH", CellPublication.SUPPRESSED, null))),
				new TeamReadinessResult.DimensionDistribution(
						AggregateStatus.PUBLISHED,
						CohortPublication.EXACT,
						5,
						List.of(new TeamReadinessResult.DimensionCell("FATIGUE", CellPublication.PUBLISHED, 5))),
				new TeamReadinessResult.AvailabilityAggregate("UNSUPPORTED"));

		TeamReadinessResponse response = TeamReadinessResponse.from(result);

		assertThat(response.teamId()).isEqualTo(teamId);
		assertThat(response.includedCount()).isNull();
		assertThat(response.categoryDistribution().cells().get(0).publication()).isEqualTo("SUPPRESSED");
		assertThat(response.categoryDistribution().cells().get(0).count()).isNull();
		assertThat(response.limitingDimensionDistribution().cells().get(0).count()).isEqualTo(5);
		assertThat(response.availability().status()).isEqualTo("UNSUPPORTED");
	}

}
