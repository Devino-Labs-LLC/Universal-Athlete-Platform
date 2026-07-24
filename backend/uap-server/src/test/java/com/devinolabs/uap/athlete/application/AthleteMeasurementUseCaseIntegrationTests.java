package com.devinolabs.uap.athlete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalType;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;
import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.SportType;
import com.devinolabs.uap.athlete.domain.Weight;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AthleteMeasurementUseCaseIntegrationTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private AddAthleteSportUseCase addAthleteSportUseCase;

	@Autowired
	private CreateAthleteGoalUseCase createAthleteGoalUseCase;

	@Autowired
	private RecordAthleteMeasurementUseCase recordAthleteMeasurementUseCase;

	@Autowired
	private ListCurrentAthleteMeasurementsUseCase listCurrentAthleteMeasurementsUseCase;

	@Autowired
	private GetCurrentAthleteMeasurementUseCase getCurrentAthleteMeasurementUseCase;

	@Autowired
	private UpdateAthleteMeasurementUseCase updateAthleteMeasurementUseCase;

	@Autowired
	private DeleteAthleteMeasurementUseCase deleteAthleteMeasurementUseCase;

	@Autowired
	private AthleteRepository athleteRepository;

	@Autowired
	private AthleteMeasurementRepository measurementRepository;

	@Test
	void recordsListsFiltersUpdatesAndDeletesMeasurements() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteSportResult sport = addAthleteSportUseCase.execute(
				accountId, SportType.SOCCER, null, true, ParticipationLevel.INTERMEDIATE, null, 3, SeasonStatus.IN_SEASON);
		AthleteGoalResult goal = createAthleteGoalUseCase.execute(
				accountId, GoalType.IMPROVE_STRENGTH, null, "Strength", null, GoalPriority.MEDIUM,
				null, null, null, null, sport.id());

		AthleteMeasurementResult older = recordAthleteMeasurementUseCase.execute(
				accountId, MeasurementType.BODY_WEIGHT, null, new BigDecimal("80.0000"), MeasurementUnit.KILOGRAM,
				null, MeasurementSource.MANUAL, "day 1", Instant.parse("2026-07-20T10:00:00Z"),
				sport.id().value(), goal.id().value());
		AthleteMeasurementResult newer = recordAthleteMeasurementUseCase.execute(
				accountId, MeasurementType.BODY_WEIGHT, null, new BigDecimal("79.5000"), MeasurementUnit.KILOGRAM,
				null, MeasurementSource.WEARABLE, null, Instant.parse("2026-07-22T10:00:00Z"),
				null, null);
		recordAthleteMeasurementUseCase.execute(
				accountId, MeasurementType.SESSION_RPE, null, new BigDecimal("6"), MeasurementUnit.SCORE,
				null, MeasurementSource.MANUAL, null, Instant.parse("2026-07-21T10:00:00Z"),
				sport.id().value(), null);

		List<AthleteMeasurementResult> ordered = listCurrentAthleteMeasurementsUseCase.execute(
				accountId, null, null, null, null, null, null);
		assertThat(ordered).extracting(AthleteMeasurementResult::measuredAt)
				.containsExactly(
						Instant.parse("2026-07-22T10:00:00Z"),
						Instant.parse("2026-07-21T10:00:00Z"),
						Instant.parse("2026-07-20T10:00:00Z"));
		assertThat(ordered).extracting(AthleteMeasurementResult::id)
				.containsExactly(newer.id(), ordered.get(1).id(), older.id());

		assertThat(listCurrentAthleteMeasurementsUseCase.execute(
				accountId, MeasurementType.BODY_WEIGHT, null, null, null, null, null)).hasSize(2);
		assertThat(listCurrentAthleteMeasurementsUseCase.execute(
				accountId, null, MeasurementSource.WEARABLE, null, null, null, null)).hasSize(1);
		assertThat(listCurrentAthleteMeasurementsUseCase.execute(
				accountId, null, null, sport.id().value(), null, null, null)).hasSize(2);
		assertThat(listCurrentAthleteMeasurementsUseCase.execute(
				accountId, null, null, null, goal.id().value(), null, null)).hasSize(1);
		assertThat(listCurrentAthleteMeasurementsUseCase.execute(
				accountId, null, null, null, null,
				Instant.parse("2026-07-21T00:00:00Z"),
				Instant.parse("2026-07-22T23:59:59Z"))).hasSize(2);

		assertThatThrownBy(() -> listCurrentAthleteMeasurementsUseCase.execute(
				accountId, null, null, null, null,
				Instant.parse("2026-07-23T00:00:00Z"),
				Instant.parse("2026-07-20T00:00:00Z")))
				.isInstanceOf(InvalidMeasurementDateRangeException.class);

		AthleteMeasurementResult updated = updateAthleteMeasurementUseCase.execute(
				accountId,
				newer.id(),
				new UpdateAthleteMeasurementCommand(
						new BigDecimal("79.2500"), true,
						null, false,
						null, false,
						"corrected", true,
						Instant.parse("2026-07-22T11:00:00Z"), true,
						sport.id().value(), true,
						goal.id().value(), true));
		assertThat(updated.value()).isEqualByComparingTo("79.2500");
		assertThat(updated.notes()).isEqualTo("corrected");
		assertThat(updated.athleteSportId()).isEqualTo(sport.id());
		assertThat(updated.athleteGoalId()).isEqualTo(goal.id());

		AthleteMeasurementResult cleared = updateAthleteMeasurementUseCase.execute(
				accountId,
				newer.id(),
				new UpdateAthleteMeasurementCommand(
						null, false,
						null, false,
						null, false,
						null, true,
						null, false,
						null, true,
						null, true));
		assertThat(cleared.notes()).isNull();
		assertThat(cleared.athleteSportId()).isNull();
		assertThat(cleared.athleteGoalId()).isNull();
		assertThat(cleared.value()).isEqualByComparingTo("79.2500");

		deleteAthleteMeasurementUseCase.execute(accountId, older.id());
		assertThatThrownBy(() -> getCurrentAthleteMeasurementUseCase.execute(accountId, older.id()))
				.isInstanceOf(AthleteMeasurementNotFoundException.class);
	}

	@Test
	void rejectsCrossAccountLinksArchivedAthleteAndEnforcesIsolation() {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();
		createAthlete(owner);
		createAthlete(other);

		AthleteSportResult otherSport = addAthleteSportUseCase.execute(
				other, SportType.TENNIS, null, true, ParticipationLevel.BEGINNER, null, 1, SeasonStatus.OFF_SEASON);
		AthleteGoalResult otherGoal = createAthleteGoalUseCase.execute(
				other, GoalType.GENERAL_FITNESS, null, "Fit", null, GoalPriority.LOW,
				null, null, null, null, null);

		assertThatThrownBy(() -> recordAthleteMeasurementUseCase.execute(
				owner, MeasurementType.BODY_WEIGHT, null, new BigDecimal("70"), MeasurementUnit.KILOGRAM,
				null, null, null, Instant.parse("2026-07-20T10:00:00Z"),
				otherSport.id().value(), null)).isInstanceOf(AthleteSportNotFoundException.class);
		assertThatThrownBy(() -> recordAthleteMeasurementUseCase.execute(
				owner, MeasurementType.BODY_WEIGHT, null, new BigDecimal("70"), MeasurementUnit.KILOGRAM,
				null, null, null, Instant.parse("2026-07-20T10:00:00Z"),
				null, otherGoal.id().value())).isInstanceOf(AthleteGoalNotFoundException.class);
		assertThatThrownBy(() -> recordAthleteMeasurementUseCase.execute(
				owner, MeasurementType.BODY_WEIGHT, null, new BigDecimal("70"), MeasurementUnit.KILOGRAM,
				null, null, null, Instant.parse("2026-07-20T10:00:00Z"),
				AthleteSportId.generate().value(), null)).isInstanceOf(AthleteSportNotFoundException.class);

		AthleteMeasurementResult ownerMeasurement = recordAthleteMeasurementUseCase.execute(
				owner, MeasurementType.BODY_WEIGHT, null, new BigDecimal("70"), MeasurementUnit.KILOGRAM,
				null, null, null, Instant.parse("2026-07-20T10:00:00Z"), null, null);
		assertThatThrownBy(() -> getCurrentAthleteMeasurementUseCase.execute(other, ownerMeasurement.id()))
				.isInstanceOf(AthleteMeasurementNotFoundException.class);

		Athlete athlete = athleteRepository.findByAccountId(owner).orElseThrow();
		athlete.archive(CLOCK);
		athleteRepository.save(athlete);
		assertThatThrownBy(() -> recordAthleteMeasurementUseCase.execute(
				owner, MeasurementType.BODY_WEIGHT, null, new BigDecimal("71"), MeasurementUnit.KILOGRAM,
				null, null, null, Instant.parse("2026-07-20T10:00:00Z"), null, null))
				.isInstanceOf(AthleteArchivedException.class);
	}

	@Test
	void optimisticLockingAndDecimalRoundTrip() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteId athleteId = athleteRepository.findByAccountId(accountId).orElseThrow().id();

		AthleteMeasurementResult created = recordAthleteMeasurementUseCase.execute(
				accountId, MeasurementType.VO2_MAX, null, new BigDecimal("48.1234"),
				MeasurementUnit.MILLILITER_PER_KILOGRAM_PER_MINUTE, null, MeasurementSource.CLINICAL, null,
				Instant.parse("2026-07-20T10:00:00Z"), null, null);

		AthleteMeasurement loaded = measurementRepository.findByIdAndAthleteId(created.id(), athleteId).orElseThrow();
		assertThat(loaded.value()).isEqualByComparingTo("48.1234");
		assertThat(loaded.value().scale()).isEqualTo(4);
		assertThat(loaded.version()).isZero();

		loaded.correctValue(new BigDecimal("49.0000"), CLOCK);
		AthleteMeasurement saved = measurementRepository.save(loaded);
		assertThat(saved.version()).isEqualTo(1L);

		AthleteMeasurement stale = AthleteMeasurement.rehydrate(
				loaded.id(),
				loaded.athleteId(),
				loaded.measurementType(),
				loaded.customMeasurementName(),
				new BigDecimal("47.0000"),
				loaded.unit(),
				loaded.customUnit(),
				loaded.source(),
				loaded.notes(),
				loaded.measuredAt(),
				loaded.athleteSportId(),
				loaded.athleteGoalId(),
				loaded.createdAt(),
				loaded.updatedAt(),
				0L);
		assertThatThrownBy(() -> measurementRepository.save(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	private void createAthlete(AccountId accountId) {
		createAthleteProfileUseCase.execute(
				accountId,
				"Jordan",
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
	}

}
