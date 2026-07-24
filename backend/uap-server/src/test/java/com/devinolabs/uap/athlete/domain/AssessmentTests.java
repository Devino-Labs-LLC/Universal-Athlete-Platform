package com.devinolabs.uap.athlete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AssessmentTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);
	private static final Clock AFTER = Clock.fixed(Instant.parse("2026-07-24T17:00:00Z"), ZoneOffset.UTC);

	@Test
	void createsPlannedAssessmentWithNormalizedFields() {
		Assessment assessment = Assessment.create(
				AssessmentId.generate(),
				AthleteId.generate(),
				AssessmentType.VERTICAL_JUMP,
				null,
				"  Baseline   Jump  ",
				"  Initial screen  ",
				Instant.parse("2026-08-01T10:00:00Z"),
				"  bring shoes  ",
				null,
				null,
				CLOCK);

		assertThat(assessment.title()).isEqualTo("Baseline   Jump");
		assertThat(assessment.normalizedTitle()).isEqualTo("baseline jump");
		assertThat(assessment.description()).isEqualTo("Initial screen");
		assertThat(assessment.notes()).isEqualTo("bring shoes");
		assertThat(assessment.status()).isEqualTo(AssessmentStatus.PLANNED);
		assertThat(assessment.startedAt()).isNull();
		assertThat(assessment.completedAt()).isNull();
		assertThat(assessment.createdAt()).isEqualTo(Instant.parse("2026-07-24T15:00:00Z"));
		assertThat(assessment.version()).isZero();
	}

	@Test
	void requiresAndRejectsCustomTypeNameForOther() {
		Assessment other = Assessment.create(
				AssessmentId.generate(),
				AthleteId.generate(),
				AssessmentType.OTHER,
				"  Custom Screen  ",
				"Custom",
				null,
				null,
				null,
				null,
				null,
				CLOCK);
		assertThat(other.customTypeName()).isEqualTo("Custom Screen");

		assertThatThrownBy(() -> Assessment.create(
				AssessmentId.generate(),
				AthleteId.generate(),
				AssessmentType.OTHER,
				" ",
				"Custom",
				null,
				null,
				null,
				null,
				null,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> Assessment.create(
				AssessmentId.generate(),
				AthleteId.generate(),
				AssessmentType.STRENGTH,
				"Not allowed",
				"Strength",
				null,
				null,
				null,
				null,
				null,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void normalizesBlankOptionalTextAndRejectsOversizedFields() {
		Assessment blankOptional = Assessment.create(
				AssessmentId.generate(),
				AthleteId.generate(),
				AssessmentType.MOBILITY,
				null,
				"Mobility",
				"   ",
				null,
				"   ",
				null,
				null,
				CLOCK);
		assertThat(blankOptional.description()).isNull();
		assertThat(blankOptional.notes()).isNull();

		assertThatThrownBy(() -> Assessment.create(
				AssessmentId.generate(), AthleteId.generate(), AssessmentType.MOBILITY, null, "  ", null, null, null,
				null, null, CLOCK)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Assessment.create(
				AssessmentId.generate(), AthleteId.generate(), AssessmentType.MOBILITY, null, "x".repeat(161), null,
				null, null, null, null, CLOCK)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Assessment.create(
				AssessmentId.generate(), AthleteId.generate(), AssessmentType.MOBILITY, null, "ok", "d".repeat(1001),
				null, null, null, null, CLOCK)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Assessment.create(
				AssessmentId.generate(), AthleteId.generate(), AssessmentType.MOBILITY, null, "ok", null, null,
				"n".repeat(2001), null, null, CLOCK)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void supportsValidLifecycleTransitions() {
		Assessment assessment = createPlanned();

		assessment.start(LATER);
		assertThat(assessment.status()).isEqualTo(AssessmentStatus.IN_PROGRESS);
		assertThat(assessment.startedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));
		assertThat(assessment.completedAt()).isNull();

		assessment.complete(AFTER);
		assertThat(assessment.status()).isEqualTo(AssessmentStatus.COMPLETED);
		assertThat(assessment.completedAt()).isEqualTo(Instant.parse("2026-07-24T17:00:00Z"));

		assessment.reopen(AFTER);
		assertThat(assessment.status()).isEqualTo(AssessmentStatus.IN_PROGRESS);
		assertThat(assessment.startedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));
		assertThat(assessment.completedAt()).isNull();

		Assessment cancelledFromPlanned = createPlanned();
		cancelledFromPlanned.cancel(LATER);
		assertThat(cancelledFromPlanned.status()).isEqualTo(AssessmentStatus.CANCELLED);
		assertThat(cancelledFromPlanned.completedAt()).isNull();

		cancelledFromPlanned.reopen(AFTER);
		assertThat(cancelledFromPlanned.status()).isEqualTo(AssessmentStatus.PLANNED);
		assertThat(cancelledFromPlanned.startedAt()).isNull();

		Assessment cancelledFromInProgress = createPlanned();
		cancelledFromInProgress.start(LATER);
		cancelledFromInProgress.cancel(AFTER);
		assertThat(cancelledFromInProgress.status()).isEqualTo(AssessmentStatus.CANCELLED);
		assertThat(cancelledFromInProgress.startedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));
	}

	@Test
	void rejectsInvalidLifecycleTransitions() {
		Assessment planned = createPlanned();
		assertThatThrownBy(() -> planned.complete(LATER)).isInstanceOf(IllegalStateException.class);

		Assessment inProgress = createPlanned();
		inProgress.start(LATER);
		inProgress.start(AFTER);
		assertThat(inProgress.status()).isEqualTo(AssessmentStatus.IN_PROGRESS);

		Assessment completed = createPlanned();
		completed.start(LATER);
		completed.complete(AFTER);
		assertThatThrownBy(() -> completed.cancel(AFTER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> completed.start(AFTER)).isInstanceOf(IllegalStateException.class);

		Assessment cancelled = createPlanned();
		cancelled.cancel(LATER);
		assertThatThrownBy(() -> cancelled.start(AFTER)).isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> cancelled.complete(AFTER)).isInstanceOf(IllegalStateException.class);
	}

	@Test
	void enforcesStatusTimestampInvariantsOnRehydrate() {
		AssessmentId id = AssessmentId.generate();
		AthleteId athleteId = AthleteId.generate();
		Instant now = Instant.parse("2026-07-24T15:00:00Z");

		assertThatThrownBy(() -> Assessment.rehydrate(
				id, athleteId, null, null, AssessmentType.STRENGTH, null, "Strength", "strength", null,
				AssessmentStatus.PLANNED, null, now, null, null, now, now, 0L))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> Assessment.rehydrate(
				id, athleteId, null, null, AssessmentType.STRENGTH, null, "Strength", "strength", null,
				AssessmentStatus.IN_PROGRESS, null, null, null, null, now, now, 0L))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> Assessment.rehydrate(
				id, athleteId, null, null, AssessmentType.STRENGTH, null, "Strength", "strength", null,
				AssessmentStatus.COMPLETED, null, now, now.minusSeconds(1), null, now, now, 0L))
				.isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> Assessment.rehydrate(
				id, athleteId, null, null, AssessmentType.STRENGTH, null, "Strength", "strength", null,
				AssessmentStatus.CANCELLED, null, null, now, null, now, now, 0L))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void duplicateCandidateExcludesCancelled() {
		Assessment planned = createPlanned();
		assertThat(planned.isDuplicateCandidate()).isTrue();
		planned.cancel(LATER);
		assertThat(planned.isDuplicateCandidate()).isFalse();
	}

	@Test
	void renameAndNotesUseClockTimestamps() {
		Assessment assessment = createPlanned();
		assessment.rename("  Updated   Title ", LATER);
		assertThat(assessment.title()).isEqualTo("Updated   Title");
		assertThat(assessment.normalizedTitle()).isEqualTo("updated title");
		assertThat(assessment.updatedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));

		assessment.changeNotes("  new notes  ", AFTER);
		assertThat(assessment.notes()).isEqualTo("new notes");
		assertThat(assessment.updatedAt()).isEqualTo(Instant.parse("2026-07-24T17:00:00Z"));
	}

	private static Assessment createPlanned() {
		return Assessment.create(
				AssessmentId.generate(),
				AthleteId.generate(),
				AssessmentType.STRENGTH,
				null,
				"Strength Baseline",
				null,
				null,
				null,
				null,
				null,
				CLOCK);
	}

}
