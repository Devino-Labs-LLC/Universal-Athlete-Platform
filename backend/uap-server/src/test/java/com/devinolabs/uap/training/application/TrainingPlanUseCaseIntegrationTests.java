package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import com.devinolabs.uap.athlete.api.AthleteArchivedException;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.application.AddAthleteSportUseCase;
import com.devinolabs.uap.athlete.application.AthleteRepository;
import com.devinolabs.uap.athlete.application.AthleteSportResult;
import com.devinolabs.uap.athlete.application.CreateAthleteGoalUseCase;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.application.AthleteGoalResult;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalType;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.SportType;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanStatusAction;
import com.devinolabs.uap.training.domain.TrainingPlanType;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TrainingPlanUseCaseIntegrationTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private AddAthleteSportUseCase addAthleteSportUseCase;

	@Autowired
	private CreateAthleteGoalUseCase createAthleteGoalUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private ListTrainingPlansUseCase listTrainingPlansUseCase;

	@Autowired
	private GetTrainingPlanUseCase getTrainingPlanUseCase;

	@Autowired
	private UpdateTrainingPlanUseCase updateTrainingPlanUseCase;

	@Autowired
	private ChangeTrainingPlanStatusUseCase changeTrainingPlanStatusUseCase;

	@Autowired
	private DeleteTrainingPlanUseCase deleteTrainingPlanUseCase;

	@Autowired
	private AthleteRepository athleteRepository;

	@Autowired
	private TrainingPlanRepository trainingPlanRepository;

	@Test
	void createsListsUpdatesDeletesAndEnforcesLifecycle() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteSportResult sport = addAthleteSportUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				SportType.BASKETBALL, null, true, ParticipationLevel.COLLEGIATE, null, 4, SeasonStatus.IN_SEASON);
		AthleteGoalResult goal = createAthleteGoalUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				GoalType.IMPROVE_STRENGTH, null, "Power", null, GoalPriority.MEDIUM,
				null, null, null, null, sport.id());

		TrainingPlanResult created = createTrainingPlanUseCase.execute(
				accountId,
				TrainingPlanType.VERTICAL,
				null,
				"  Summer   Vertical  ",
				"Jump block",
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 8, 31),
				sport.id().value(),
				goal.id().value());
		assertThat(created.status()).isEqualTo(TrainingPlanStatus.DRAFT);
		assertThat(created.name()).isEqualTo("Summer   Vertical");
		assertThat(created.athleteSportId().value()).isEqualTo(sport.id().value());

		createTrainingPlanUseCase.execute(
				accountId,
				TrainingPlanType.STRENGTH,
				null,
				"Offseason Strength",
				null,
				LocalDate.of(2026, 9, 1),
				LocalDate.of(2026, 11, 30),
				null,
				null);

		List<TrainingPlanResult> listed = listTrainingPlansUseCase.execute(accountId, null, null);
		assertThat(listed).hasSize(2);
		assertThat(listed.getFirst().startDate()).isEqualTo(LocalDate.of(2026, 9, 1));

		assertThat(listTrainingPlansUseCase.execute(accountId, TrainingPlanStatus.DRAFT, TrainingPlanType.VERTICAL))
				.hasSize(1);

		TrainingPlanResult updated = updateTrainingPlanUseCase.execute(
				accountId,
				created.id(),
				new UpdateTrainingPlanCommand(
						"Summer Vertical Program", true,
						null, true,
						LocalDate.of(2026, 6, 15), true,
						null, false,
						null, true,
						null, true));
		assertThat(updated.name()).isEqualTo("Summer Vertical Program");
		assertThat(updated.description()).isNull();
		assertThat(updated.athleteSportId()).isNull();
		assertThat(updated.startDate()).isEqualTo(LocalDate.of(2026, 6, 15));

		changeTrainingPlanStatusUseCase.execute(accountId, created.id(), TrainingPlanStatusAction.ACTIVATE);
		assertThatThrownBy(() -> deleteTrainingPlanUseCase.execute(accountId, created.id()))
				.isInstanceOf(TrainingPlanDeleteNotAllowedException.class);

		changeTrainingPlanStatusUseCase.execute(accountId, created.id(), TrainingPlanStatusAction.COMPLETE);
		changeTrainingPlanStatusUseCase.execute(accountId, created.id(), TrainingPlanStatusAction.ARCHIVE);
		assertThat(getTrainingPlanUseCase.execute(accountId, created.id()).status())
				.isEqualTo(TrainingPlanStatus.ARCHIVED);

		TrainingPlanResult draft = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.GENERAL, null, "Delete Me", null,
				LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1), null, null);
		deleteTrainingPlanUseCase.execute(accountId, draft.id());
		assertThatThrownBy(() -> getTrainingPlanUseCase.execute(accountId, draft.id()))
				.isInstanceOf(TrainingPlanNotFoundException.class);
	}

	@Test
	void rejectsOverlappingDuplicatesInvalidStatusAndArchivedAthlete() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);

		createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength Block", null,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31), null, null);

		assertThatThrownBy(() -> createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, " strength   block ", null,
				LocalDate.of(2026, 3, 1), LocalDate.of(2026, 4, 30), null, null))
				.isInstanceOf(DuplicateTrainingPlanException.class);

		TrainingPlanResult nonOverlap = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "strength block", null,
				LocalDate.of(2026, 4, 1), LocalDate.of(2026, 5, 31), null, null);
		assertThat(nonOverlap.name()).isEqualTo("strength block");

		TrainingPlanResult archived = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.ENDURANCE, null, "Base Phase", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 1), null, null);
		changeTrainingPlanStatusUseCase.execute(accountId, archived.id(), TrainingPlanStatusAction.ARCHIVE);
		TrainingPlanResult allowedAfterArchive = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.ENDURANCE, null, "Base Phase", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 1), null, null);
		assertThat(allowedAfterArchive.id()).isNotEqualTo(archived.id());

		assertThatThrownBy(() -> changeTrainingPlanStatusUseCase.execute(
				accountId, nonOverlap.id(), TrainingPlanStatusAction.COMPLETE))
				.isInstanceOf(InvalidTrainingPlanStatusException.class);

		assertThatThrownBy(() -> createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.OTHER, null, "Custom", null,
				LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 1), null, null))
				.isInstanceOf(InvalidCustomTrainingPlanTypeException.class);

		assertThatThrownBy(() -> createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Bad Dates", null,
				LocalDate.of(2026, 12, 1), LocalDate.of(2026, 11, 1), null, null))
				.isInstanceOf(InvalidTrainingPlanDatesException.class);

		AccountId missing = AccountId.generate();
		assertThatThrownBy(() -> listTrainingPlansUseCase.execute(missing, null, null))
				.isInstanceOf(AthleteNotFoundException.class);

		var athlete = athleteRepository.findByAccountId(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value())).orElseThrow();
		athlete.archive(CLOCK);
		athleteRepository.save(athlete);
		assertThatThrownBy(() -> createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.GENERAL, null, "After Archive", null,
				LocalDate.of(2027, 1, 1), LocalDate.of(2027, 2, 1), null, null))
				.isInstanceOf(AthleteArchivedException.class);
	}

	@Test
	void enforcesOwnershipIsolationAndOptimisticLocking() {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();
		createAthlete(owner);
		createAthlete(other);

		AthleteSportResult otherSport = addAthleteSportUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(other.value()),
				SportType.TENNIS, null, true, ParticipationLevel.BEGINNER, null, 1, SeasonStatus.OFF_SEASON);

		assertThatThrownBy(() -> createTrainingPlanUseCase.execute(
				owner, TrainingPlanType.SPORT_SPECIFIC, null, "Linked", null,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1),
				otherSport.id().value(), null))
				.isInstanceOf(com.devinolabs.uap.athlete.api.AthleteSportNotOwnedException.class);

		TrainingPlanResult ownerPlan = createTrainingPlanUseCase.execute(
				owner, TrainingPlanType.POWER, null, "Power", null,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1), null, null);
		assertThatThrownBy(() -> getTrainingPlanUseCase.execute(other, ownerPlan.id()))
				.isInstanceOf(TrainingPlanNotFoundException.class);

		TrainingPlan loaded = trainingPlanRepository
				.findByIdAndAthleteId(ownerPlan.id(), com.devinolabs.uap.training.domain.AthleteId.of(
						athleteRepository.findByAccountId(
								com.devinolabs.uap.athlete.domain.AccountId.of(owner.value()))
								.orElseThrow().id().value()))
				.orElseThrow();
		assertThat(loaded.version()).isZero();
		loaded.rename("Power Updated", CLOCK);
		TrainingPlan saved = trainingPlanRepository.save(loaded);
		assertThat(saved.version()).isEqualTo(1L);

		TrainingPlan stale = TrainingPlan.rehydrate(
				loaded.id(),
				loaded.athleteId(),
				loaded.athleteSportId(),
				loaded.athleteGoalId(),
				"Stale",
				"stale",
				loaded.description(),
				loaded.type(),
				loaded.customTypeName(),
				loaded.status(),
				loaded.startDate(),
				loaded.endDate(),
				loaded.scheduleStartDate(),
				loaded.scheduleEndDate(),
				loaded.scheduleTimezone(),
				loaded.scheduleStatus(),
				loaded.recurrenceMode(),
				loaded.scheduleGeneratedThrough(),
				loaded.scheduleActivatedAt(),
				loaded.schedulePausedAt(),
				loaded.createdAt(),
				loaded.updatedAt(),
				0L);
		assertThatThrownBy(() -> trainingPlanRepository.save(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	private void createAthlete(AccountId accountId) {
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
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
