package com.devinolabs.uap.training.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TrainingAdjustmentApplicabilityResolverTests {

	@Test
	void mapsConcretePreferencesAsConcretelyApplicable() {
		assertThat(TrainingAdjustmentApplicabilityResolver.resolve(
				TrainingAdjustmentType.PREFER_LOWER_IMPACT_VARIATIONS))
				.isEqualTo(TrainingAdjustmentApplicability.CONCRETELY_APPLICABLE);
		assertThat(TrainingAdjustmentApplicabilityResolver.resolve(
				TrainingAdjustmentType.PREFER_EQUIPMENT_COMPATIBLE_VARIATIONS))
				.isEqualTo(TrainingAdjustmentApplicability.CONCRETELY_APPLICABLE);
	}

	@Test
	void mapsVolumeIntensityDurationRestAndRecoveryAsContextOnly() {
		assertThat(TrainingAdjustmentApplicabilityResolver.resolve(TrainingAdjustmentType.REDUCE_TOTAL_VOLUME))
				.isEqualTo(TrainingAdjustmentApplicability.CONTEXT_ONLY);
		assertThat(TrainingAdjustmentApplicabilityResolver.resolve(TrainingAdjustmentType.REDUCE_INTENSITY))
				.isEqualTo(TrainingAdjustmentApplicability.CONTEXT_ONLY);
		assertThat(TrainingAdjustmentApplicabilityResolver.resolve(TrainingAdjustmentType.REDUCE_SESSION_DURATION))
				.isEqualTo(TrainingAdjustmentApplicability.CONTEXT_ONLY);
		assertThat(TrainingAdjustmentApplicabilityResolver.resolve(TrainingAdjustmentType.INCREASE_REST))
				.isEqualTo(TrainingAdjustmentApplicability.CONTEXT_ONLY);
		assertThat(TrainingAdjustmentApplicabilityResolver.resolve(TrainingAdjustmentType.OPTIONAL_RECOVERY_FOCUS))
				.isEqualTo(TrainingAdjustmentApplicability.CONTEXT_ONLY);
	}

	@Test
	void mapsPreserveAndNoAdjustmentAsNotApplicable() {
		assertThat(TrainingAdjustmentApplicabilityResolver.resolve(TrainingAdjustmentType.PRESERVE_PLANNED_SESSION))
				.isEqualTo(TrainingAdjustmentApplicability.NOT_APPLICABLE);
		assertThat(TrainingAdjustmentApplicabilityResolver.resolve(TrainingAdjustmentType.NO_ADJUSTMENT))
				.isEqualTo(TrainingAdjustmentApplicability.NOT_APPLICABLE);
	}

}
