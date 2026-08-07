package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ImpactLevelOrderingTests {

	@Test
	void ranksImpactLevelsInExplicitFactualOrder() {
		assertThat(ImpactLevelOrdering.rank(ImpactLevel.NO_IMPACT)).isLessThan(
				ImpactLevelOrdering.rank(ImpactLevel.LOW_IMPACT));
		assertThat(ImpactLevelOrdering.rank(ImpactLevel.LOW_IMPACT)).isLessThan(
				ImpactLevelOrdering.rank(ImpactLevel.MODERATE_IMPACT));
		assertThat(ImpactLevelOrdering.rank(ImpactLevel.MODERATE_IMPACT)).isLessThan(
				ImpactLevelOrdering.rank(ImpactLevel.HIGH_IMPACT));
	}

	@Test
	void relativePreferenceGroupsLowerSameThenHigher() {
		assertThat(ImpactLevelOrdering.relativePreferenceGroup(
				ImpactLevel.HIGH_IMPACT, ImpactLevel.LOW_IMPACT)).isZero();
		assertThat(ImpactLevelOrdering.relativePreferenceGroup(
				ImpactLevel.HIGH_IMPACT, ImpactLevel.HIGH_IMPACT)).isEqualTo(1);
		assertThat(ImpactLevelOrdering.relativePreferenceGroup(
				ImpactLevel.MODERATE_IMPACT, ImpactLevel.HIGH_IMPACT)).isEqualTo(2);
	}

}
