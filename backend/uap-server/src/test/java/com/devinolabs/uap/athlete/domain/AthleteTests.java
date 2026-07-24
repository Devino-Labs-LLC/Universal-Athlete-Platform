package com.devinolabs.uap.athlete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AthleteTests {

	private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER_CLOCK = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void registerCreatesActiveAthleteWithInitializedTimestamps() {
		AthleteId id = AthleteId.generate();
		AccountId accountId = AccountId.generate();

		Athlete athlete = register(id, accountId);

		assertThat(athlete.id()).isEqualTo(id);
		assertThat(athlete.accountId()).isEqualTo(accountId);
		assertThat(athlete.firstName()).isEqualTo("Jordan");
		assertThat(athlete.lastName()).isEqualTo("Lee");
		assertThat(athlete.dateOfBirth()).isEqualTo(LocalDate.of(1998, 5, 12));
		assertThat(athlete.sex()).isEqualTo(Sex.FEMALE);
		assertThat(athlete.height()).isEqualTo(Height.ofCentimeters(175));
		assertThat(athlete.weight()).isEqualTo(Weight.ofKilograms(68));
		assertThat(athlete.dominantHand()).isEqualTo(DominantHand.RIGHT);
		assertThat(athlete.dominantFoot()).isEqualTo(DominantFoot.RIGHT);
		assertThat(athlete.status()).isEqualTo(AthleteStatus.ACTIVE);
		assertThat(athlete.createdAt()).isEqualTo(Instant.parse("2026-07-24T15:00:00Z"));
		assertThat(athlete.updatedAt()).isEqualTo(Instant.parse("2026-07-24T15:00:00Z"));
		assertThat(athlete.version()).isZero();
	}

	@Test
	void registerRejectsMissingOrBlankRequiredValues() {
		AthleteId id = AthleteId.generate();
		AccountId accountId = AccountId.generate();

		assertThatThrownBy(() -> Athlete.register(
				null, accountId, "A", "B", LocalDate.of(2000, 1, 1), Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(80), DominantHand.RIGHT, DominantFoot.LEFT, FIXED_CLOCK))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Athlete.register(
				id, null, "A", "B", LocalDate.of(2000, 1, 1), Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(80), DominantHand.RIGHT, DominantFoot.LEFT, FIXED_CLOCK))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Athlete.register(
				id, accountId, " ", "B", LocalDate.of(2000, 1, 1), Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(80), DominantHand.RIGHT, DominantFoot.LEFT, FIXED_CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Athlete.register(
				id, accountId, "A", "", LocalDate.of(2000, 1, 1), Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(80), DominantHand.RIGHT, DominantFoot.LEFT, FIXED_CLOCK))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Athlete.register(
				id, accountId, "A", "B", null, Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(80), DominantHand.RIGHT, DominantFoot.LEFT, FIXED_CLOCK))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Athlete.register(
				id, accountId, "A", "B", LocalDate.of(2000, 1, 1), Sex.MALE,
				null, Weight.ofKilograms(80), DominantHand.RIGHT, DominantFoot.LEFT, FIXED_CLOCK))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> Athlete.register(
				id, accountId, "A", "B", LocalDate.of(2000, 1, 1), Sex.MALE,
				Height.ofCentimeters(180), null, DominantHand.RIGHT, DominantFoot.LEFT, FIXED_CLOCK))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void registerTrimsNames() {
		Athlete athlete = Athlete.register(
				AthleteId.generate(),
				AccountId.generate(),
				"  Jordan ",
				" Lee  ",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT,
				FIXED_CLOCK);

		assertThat(athlete.firstName()).isEqualTo("Jordan");
		assertThat(athlete.lastName()).isEqualTo("Lee");
	}

	@Test
	void renameUpdatesNamesAndTimestamp() {
		Athlete athlete = register(AthleteId.generate(), AccountId.generate());

		athlete.rename("Alex", "Rivera", LATER_CLOCK);

		assertThat(athlete.firstName()).isEqualTo("Alex");
		assertThat(athlete.lastName()).isEqualTo("Rivera");
		assertThat(athlete.updatedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));
	}

	@Test
	void updateHeightAndWeightPreserveInvariants() {
		Athlete athlete = register(AthleteId.generate(), AccountId.generate());

		athlete.updateHeight(Height.ofFeetInches(6, 0), LATER_CLOCK);
		athlete.updateWeight(Weight.ofPounds(170), LATER_CLOCK);
		athlete.updateDominantHand(DominantHand.LEFT, LATER_CLOCK);
		athlete.updateDominantFoot(DominantFoot.BOTH, LATER_CLOCK);

		assertThat(athlete.height()).isEqualTo(Height.ofFeetInches(6, 0));
		assertThat(athlete.weight()).isEqualTo(Weight.ofPounds(170));
		assertThat(athlete.dominantHand()).isEqualTo(DominantHand.LEFT);
		assertThat(athlete.dominantFoot()).isEqualTo(DominantFoot.BOTH);
		assertThat(athlete.updatedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));
		assertThatThrownBy(() -> athlete.updateHeight(null, LATER_CLOCK))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> athlete.updateWeight(null, LATER_CLOCK))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	void archiveAndReactivateToggleStatus() {
		Athlete athlete = register(AthleteId.generate(), AccountId.generate());

		athlete.archive(LATER_CLOCK);
		assertThat(athlete.status()).isEqualTo(AthleteStatus.ARCHIVED);
		assertThat(athlete.updatedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));

		assertThatThrownBy(() -> athlete.archive(LATER_CLOCK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already archived");
		assertThatThrownBy(() -> athlete.rename("A", "B", LATER_CLOCK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Archived");
		assertThatThrownBy(() -> athlete.updateHeight(Height.ofCentimeters(180), LATER_CLOCK))
				.isInstanceOf(IllegalStateException.class);
		assertThatThrownBy(() -> athlete.updateWeight(Weight.ofKilograms(80), LATER_CLOCK))
				.isInstanceOf(IllegalStateException.class);

		athlete.reactivate(FIXED_CLOCK);
		assertThat(athlete.status()).isEqualTo(AthleteStatus.ACTIVE);
		assertThatThrownBy(() -> athlete.reactivate(FIXED_CLOCK))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("already active");
	}

	@Test
	void reactivateAllowsInactiveAthletes() {
		Athlete athlete = Athlete.rehydrate(
				AthleteId.generate(),
				AccountId.generate(),
				"Jordan",
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT,
				AthleteStatus.INACTIVE,
				Instant.parse("2026-07-24T15:00:00Z"),
				Instant.parse("2026-07-24T15:00:00Z"),
				1L);

		athlete.reactivate(LATER_CLOCK);

		assertThat(athlete.status()).isEqualTo(AthleteStatus.ACTIVE);
	}

	private static Athlete register(AthleteId id, AccountId accountId) {
		return Athlete.register(
				id,
				accountId,
				"Jordan",
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT,
				FIXED_CLOCK);
	}

}
