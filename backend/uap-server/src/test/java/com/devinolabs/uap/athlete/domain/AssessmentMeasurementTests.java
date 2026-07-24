package com.devinolabs.uap.athlete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AssessmentMeasurementTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void attachesWithNormalizedOptionalFieldsAndValidDisplayOrder() {
		AssessmentMeasurement attachment = AssessmentMeasurement.attach(
				AssessmentMeasurementId.generate(),
				AssessmentId.generate(),
				AthleteId.generate(),
				AthleteMeasurementId.generate(),
				0,
				"  Primary  ",
				"  note  ",
				CLOCK);

		assertThat(attachment.label()).isEqualTo("Primary");
		assertThat(attachment.notes()).isEqualTo("note");
		assertThat(attachment.displayOrder()).isZero();
		assertThat(attachment.isSnapshotted()).isFalse();
		assertThat(attachment.snapshot()).isNull();
		assertThat(attachment.version()).isZero();

		AssessmentMeasurement blankOptional = AssessmentMeasurement.attach(
				AssessmentMeasurementId.generate(),
				AssessmentId.generate(),
				AthleteId.generate(),
				AthleteMeasurementId.generate(),
				1,
				"   ",
				"   ",
				CLOCK);
		assertThat(blankOptional.label()).isNull();
		assertThat(blankOptional.notes()).isNull();
	}

	@Test
	void rejectsNegativeDisplayOrderAndOversizedText() {
		assertThatThrownBy(() -> AssessmentMeasurement.attach(
				AssessmentMeasurementId.generate(), AssessmentId.generate(), AthleteId.generate(),
				AthleteMeasurementId.generate(), -1, null, null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AssessmentMeasurement.attach(
				AssessmentMeasurementId.generate(), AssessmentId.generate(), AthleteId.generate(),
				AthleteMeasurementId.generate(), 0, "x".repeat(161), null, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AssessmentMeasurement.attach(
				AssessmentMeasurementId.generate(), AssessmentId.generate(), AthleteId.generate(),
				AthleteMeasurementId.generate(), 0, null, "n".repeat(1001), CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void captureSnapshotCopiesSourceFieldsAndAllowsOverwrite() {
		AthleteId athleteId = AthleteId.generate();
		AthleteMeasurementId sourceId = AthleteMeasurementId.generate();
		AthleteMeasurement source = AthleteMeasurement.record(
				sourceId,
				athleteId,
				MeasurementType.BODY_WEIGHT,
				null,
				new BigDecimal("80.1234"),
				MeasurementUnit.KILOGRAM,
				null,
				MeasurementSource.WEARABLE,
				null,
				Instant.parse("2026-07-20T10:00:00Z"),
				null,
				null,
				CLOCK);

		AssessmentMeasurement attachment = AssessmentMeasurement.attach(
				AssessmentMeasurementId.generate(),
				AssessmentId.generate(),
				athleteId,
				sourceId,
				0,
				null,
				null,
				CLOCK);

		attachment.captureSnapshot(source, CLOCK);
		assertThat(attachment.isSnapshotted()).isTrue();
		assertThat(attachment.snapshot().value()).isEqualByComparingTo("80.1234");
		assertThat(attachment.snapshot().value().scale()).isEqualTo(4);
		assertThat(attachment.snapshot().measurementType()).isEqualTo(MeasurementType.BODY_WEIGHT);
		assertThat(attachment.snapshot().unit()).isEqualTo(MeasurementUnit.KILOGRAM);
		assertThat(attachment.snapshot().source()).isEqualTo(MeasurementSource.WEARABLE);
		assertThat(attachment.snapshot().measuredAt()).isEqualTo(Instant.parse("2026-07-20T10:00:00Z"));
		assertThat(attachment.snapshot().snapshottedAt()).isEqualTo(Instant.parse("2026-07-24T15:00:00Z"));

		source.correctValue(new BigDecimal("79.0000"), LATER);
		attachment.captureSnapshot(source, LATER);
		assertThat(attachment.snapshot().value()).isEqualByComparingTo("79.0000");
		assertThat(attachment.snapshot().snapshottedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));
	}

	@Test
	void rejectsSnapshotFromMismatchedSourceOrAthlete() {
		AthleteId athleteId = AthleteId.generate();
		AthleteMeasurementId sourceId = AthleteMeasurementId.generate();
		AssessmentMeasurement attachment = AssessmentMeasurement.attach(
				AssessmentMeasurementId.generate(),
				AssessmentId.generate(),
				athleteId,
				sourceId,
				0,
				null,
				null,
				CLOCK);

		AthleteMeasurement otherId = AthleteMeasurement.record(
				AthleteMeasurementId.generate(),
				athleteId,
				MeasurementType.BODY_WEIGHT,
				null,
				new BigDecimal("70"),
				MeasurementUnit.KILOGRAM,
				null,
				null,
				null,
				Instant.parse("2026-07-20T10:00:00Z"),
				null,
				null,
				CLOCK);
		assertThatThrownBy(() -> attachment.captureSnapshot(otherId, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);

		AthleteMeasurement otherAthlete = AthleteMeasurement.record(
				sourceId,
				AthleteId.generate(),
				MeasurementType.BODY_WEIGHT,
				null,
				new BigDecimal("70"),
				MeasurementUnit.KILOGRAM,
				null,
				null,
				null,
				Instant.parse("2026-07-20T10:00:00Z"),
				null,
				null,
				CLOCK);
		assertThatThrownBy(() -> attachment.captureSnapshot(otherAthlete, CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void identityFieldsRemainImmutableThroughMutators() {
		AssessmentMeasurementId id = AssessmentMeasurementId.generate();
		AssessmentId assessmentId = AssessmentId.generate();
		AthleteId athleteId = AthleteId.generate();
		AthleteMeasurementId sourceId = AthleteMeasurementId.generate();
		AssessmentMeasurement attachment = AssessmentMeasurement.attach(
				id, assessmentId, athleteId, sourceId, 0, null, null, CLOCK);

		attachment.changeDisplayOrder(3, LATER);
		attachment.changeLabel("Label", LATER);
		attachment.changeNotes("Notes", LATER);

		assertThat(attachment.id()).isEqualTo(id);
		assertThat(attachment.assessmentId()).isEqualTo(assessmentId);
		assertThat(attachment.athleteId()).isEqualTo(athleteId);
		assertThat(attachment.sourceMeasurementId()).isEqualTo(sourceId);
		assertThat(attachment.displayOrder()).isEqualTo(3);
		assertThat(attachment.label()).isEqualTo("Label");
		assertThat(attachment.notes()).isEqualTo("Notes");
	}

}
