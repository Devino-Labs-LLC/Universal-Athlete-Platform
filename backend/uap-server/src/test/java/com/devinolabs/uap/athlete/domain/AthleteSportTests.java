package com.devinolabs.uap.athlete.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class AthleteSportTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);
	private static final Clock LATER = Clock.fixed(Instant.parse("2026-07-24T16:00:00Z"), ZoneOffset.UTC);

	@Test
	void registersStandardSport() {
		AthleteSport sport = AthleteSport.register(
				AthleteSportId.generate(),
				AthleteId.generate(),
				SportType.SOCCER,
				null,
				true,
				ParticipationLevel.HIGH_SCHOOL,
				" Midfielder ",
				5,
				SeasonStatus.IN_SEASON,
				CLOCK);

		assertThat(sport.sportType()).isEqualTo(SportType.SOCCER);
		assertThat(sport.customSportName()).isNull();
		assertThat(sport.primarySport()).isTrue();
		assertThat(sport.preferredPosition()).isEqualTo("Midfielder");
		assertThat(sport.yearsExperience()).isEqualTo(5);
		assertThat(sport.version()).isZero();
		assertThat(sport.sportIdentityKey()).isEqualTo("SOCCER");
	}

	@Test
	void requiresAndNormalizesOtherSportName() {
		AthleteSport sport = AthleteSport.register(
				AthleteSportId.generate(),
				AthleteId.generate(),
				SportType.OTHER,
				"  Ultimate Frisbee ",
				false,
				ParticipationLevel.RECREATIONAL,
				null,
				2,
				SeasonStatus.YEAR_ROUND,
				CLOCK);

		assertThat(sport.customSportName()).isEqualTo("Ultimate Frisbee");
		assertThat(sport.customSportNameNormalized()).isEqualTo("ultimate frisbee");
		assertThat(sport.sportIdentityKey()).isEqualTo("OTHER:ultimate frisbee");

		assertThatThrownBy(() -> AthleteSport.register(
				AthleteSportId.generate(),
				AthleteId.generate(),
				SportType.OTHER,
				" ",
				false,
				ParticipationLevel.BEGINNER,
				null,
				0,
				SeasonStatus.NOT_APPLICABLE,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteSport.register(
				AthleteSportId.generate(),
				AthleteId.generate(),
				SportType.BASKETBALL,
				"Hoops",
				false,
				ParticipationLevel.BEGINNER,
				null,
				0,
				SeasonStatus.NOT_APPLICABLE,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void rejectsInvalidYearsAndPreferredPosition() {
		assertThatThrownBy(() -> AthleteSport.register(
				AthleteSportId.generate(),
				AthleteId.generate(),
				SportType.RUNNING,
				null,
				false,
				ParticipationLevel.BEGINNER,
				null,
				-1,
				SeasonStatus.YEAR_ROUND,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteSport.register(
				AthleteSportId.generate(),
				AthleteId.generate(),
				SportType.RUNNING,
				null,
				false,
				ParticipationLevel.BEGINNER,
				null,
				81,
				SeasonStatus.YEAR_ROUND,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);

		assertThatThrownBy(() -> AthleteSport.register(
				AthleteSportId.generate(),
				AthleteId.generate(),
				SportType.RUNNING,
				null,
				false,
				ParticipationLevel.BEGINNER,
				"x".repeat(101),
				1,
				SeasonStatus.YEAR_ROUND,
				CLOCK)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void updatesParticipationAndPrimaryFlags() {
		AthleteSport sport = AthleteSport.register(
				AthleteSportId.generate(),
				AthleteId.generate(),
				SportType.TENNIS,
				null,
				false,
				ParticipationLevel.INTERMEDIATE,
				"Singles",
				3,
				SeasonStatus.OFF_SEASON,
				CLOCK);

		sport.updateParticipation(ParticipationLevel.ADVANCED, " Doubles ", 4, SeasonStatus.IN_SEASON, LATER);
		assertThat(sport.participationLevel()).isEqualTo(ParticipationLevel.ADVANCED);
		assertThat(sport.preferredPosition()).isEqualTo("Doubles");
		assertThat(sport.yearsExperience()).isEqualTo(4);
		assertThat(sport.seasonStatus()).isEqualTo(SeasonStatus.IN_SEASON);
		assertThat(sport.updatedAt()).isEqualTo(Instant.parse("2026-07-24T16:00:00Z"));

		sport.markPrimary(LATER);
		assertThat(sport.primarySport()).isTrue();
		sport.markPrimary(LATER);
		assertThat(sport.primarySport()).isTrue();
		sport.unmarkPrimary(LATER);
		assertThat(sport.primarySport()).isFalse();
	}

}
