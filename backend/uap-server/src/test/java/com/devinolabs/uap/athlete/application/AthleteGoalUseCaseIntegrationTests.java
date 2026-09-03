package com.devinolabs.uap.athlete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoal;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalStatusAction;
import com.devinolabs.uap.athlete.domain.GoalTarget;
import com.devinolabs.uap.athlete.domain.GoalTargetUnit;
import com.devinolabs.uap.athlete.domain.GoalType;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.SportType;
import com.devinolabs.uap.athlete.domain.Weight;

@SpringBootTest
@Import({ TestcontainersConfiguration.class, AthleteGoalUseCaseIntegrationTests.FixedClockConfig.class })
class AthleteGoalUseCaseIntegrationTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private AddAthleteSportUseCase addAthleteSportUseCase;

	@Autowired
	private CreateAthleteGoalUseCase createAthleteGoalUseCase;

	@Autowired
	private ListCurrentAthleteGoalsUseCase listCurrentAthleteGoalsUseCase;

	@Autowired
	private GetCurrentAthleteGoalUseCase getCurrentAthleteGoalUseCase;

	@Autowired
	private UpdateAthleteGoalUseCase updateAthleteGoalUseCase;

	@Autowired
	private ChangeAthleteGoalStatusUseCase changeAthleteGoalStatusUseCase;

	@Autowired
	private DeleteAthleteGoalUseCase deleteAthleteGoalUseCase;

	@Autowired
	private AthleteRepository athleteRepository;

	@Autowired
	private AthleteGoalRepository athleteGoalRepository;

	@Test
	void createsListsFiltersUpdatesTargetsAndLinksSports() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteSportResult soccer = addAthleteSportUseCase.execute(
				accountId, SportType.SOCCER, null, true, ParticipationLevel.INTERMEDIATE, null, 3, SeasonStatus.IN_SEASON);

		AthleteGoalResult strength = createAthleteGoalUseCase.execute(
				accountId,
				GoalType.IMPROVE_STRENGTH,
				null,
				"Bench Press",
				"Primary lift",
				GoalPriority.HIGH,
				new BigDecimal("100.000"),
				GoalTargetUnit.KILOGRAM,
				null,
				LocalDate.of(2026, 10, 1),
				soccer.id());
		assertThat(strength.status()).isEqualTo(GoalStatus.ACTIVE);
		assertThat(strength.athleteSportId()).isEqualTo(soccer.id());
		assertThat(strength.targetValue()).isEqualByComparingTo("100.000");

		AthleteGoalResult endurance = createAthleteGoalUseCase.execute(
				accountId,
				GoalType.IMPROVE_ENDURANCE,
				null,
				"10k",
				null,
				GoalPriority.MEDIUM,
				null,
				null,
				null,
				LocalDate.of(2026, 9, 1),
				null);

		AthleteGoalResult paused = createAthleteGoalUseCase.execute(
				accountId,
				GoalType.GAIN_MUSCLE,
				null,
				"Hypertrophy block",
				null,
				GoalPriority.CRITICAL,
				null,
				null,
				null,
				null,
				null);
		changeAthleteGoalStatusUseCase.execute(accountId, paused.id(), GoalStatusAction.PAUSE);

		List<AthleteGoalResult> ordered = listCurrentAthleteGoalsUseCase.execute(accountId, null, null);
		assertThat(ordered).extracting(AthleteGoalResult::title)
				.containsExactly("Bench Press", "10k", "Hypertrophy block");

		assertThat(listCurrentAthleteGoalsUseCase.execute(accountId, GoalStatus.ACTIVE, null))
				.extracting(AthleteGoalResult::title)
				.containsExactly("Bench Press", "10k");
		assertThat(listCurrentAthleteGoalsUseCase.execute(accountId, null, GoalType.IMPROVE_STRENGTH))
				.extracting(AthleteGoalResult::id)
				.containsExactly(strength.id());

		AthleteGoalResult updated = updateAthleteGoalUseCase.execute(
				accountId,
				endurance.id(),
				fullUpdate("Half Marathon", "Build aerobic base", GoalPriority.HIGH,
						new BigDecimal("21.100"), GoalTargetUnit.KILOMETER, null,
						LocalDate.of(2026, 11, 15), soccer.id().value()));
		assertThat(updated.title()).isEqualTo("Half Marathon");
		assertThat(updated.targetUnit()).isEqualTo(GoalTargetUnit.KILOMETER);
		assertThat(updated.athleteSportId()).isEqualTo(soccer.id());
	}

	@Test
	void patchOmissionPreservesFieldsAndExplicitNullClearsOptionalValues() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteSportResult sport = addAthleteSportUseCase.execute(
				accountId, SportType.RUNNING, null, true, ParticipationLevel.BEGINNER, null, 1, SeasonStatus.YEAR_ROUND);

		AthleteGoalResult created = createAthleteGoalUseCase.execute(
				accountId,
				GoalType.RUN_DISTANCE,
				null,
				"Distance block",
				"Keep it",
				GoalPriority.HIGH,
				new BigDecimal("10.000"),
				GoalTargetUnit.KILOMETER,
				null,
				LocalDate.of(2026, 11, 1),
				sport.id());

		AthleteGoalResult titleOnly = updateAthleteGoalUseCase.execute(
				accountId,
				created.id(),
				new UpdateAthleteGoalCommand(
						"Distance focus", true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false));
		assertThat(titleOnly.title()).isEqualTo("Distance focus");
		assertThat(titleOnly.description()).isEqualTo("Keep it");
		assertThat(titleOnly.priority()).isEqualTo(GoalPriority.HIGH);
		assertThat(titleOnly.targetValue()).isEqualByComparingTo("10.000");
		assertThat(titleOnly.targetDate()).isEqualTo(LocalDate.of(2026, 11, 1));
		assertThat(titleOnly.athleteSportId()).isEqualTo(sport.id());

		AthleteGoalResult cleared = updateAthleteGoalUseCase.execute(
				accountId,
				created.id(),
				new UpdateAthleteGoalCommand(
						null, false,
						null, true,
						null, false,
						null, true,
						null, true,
						null, true,
						null, true,
						null, true));
		assertThat(cleared.description()).isNull();
		assertThat(cleared.targetValue()).isNull();
		assertThat(cleared.targetUnit()).isNull();
		assertThat(cleared.targetDate()).isNull();
		assertThat(cleared.athleteSportId()).isNull();
		assertThat(cleared.title()).isEqualTo("Distance focus");
		assertThat(cleared.priority()).isEqualTo(GoalPriority.HIGH);
	}

	@Test
	void enforcesActiveDuplicatePolicyIncludingPausedAndAllowsHistoricalReplacement() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);

		AthleteGoalResult first = createAthleteGoalUseCase.execute(
				accountId, GoalType.LOSE_WEIGHT, null, "Cut 5kg", null, GoalPriority.MEDIUM,
				null, null, null, null, null);

		assertThatThrownBy(() -> createAthleteGoalUseCase.execute(
				accountId, GoalType.LOSE_WEIGHT, null, "  CUT   5KG ", null, GoalPriority.LOW,
				null, null, null, null, null)).isInstanceOf(DuplicateAthleteGoalException.class);

		changeAthleteGoalStatusUseCase.execute(accountId, first.id(), GoalStatusAction.PAUSE);
		assertThatThrownBy(() -> createAthleteGoalUseCase.execute(
				accountId, GoalType.LOSE_WEIGHT, null, "Cut 5kg", null, GoalPriority.LOW,
				null, null, null, null, null)).isInstanceOf(DuplicateAthleteGoalException.class);

		changeAthleteGoalStatusUseCase.execute(accountId, first.id(), GoalStatusAction.COMPLETE);
		AthleteGoalResult replacement = createAthleteGoalUseCase.execute(
				accountId, GoalType.LOSE_WEIGHT, null, "Cut 5kg", null, GoalPriority.HIGH,
				null, null, null, null, null);
		assertThat(replacement.status()).isEqualTo(GoalStatus.ACTIVE);

		changeAthleteGoalStatusUseCase.execute(accountId, replacement.id(), GoalStatusAction.CANCEL);
		AthleteGoalResult again = createAthleteGoalUseCase.execute(
				accountId, GoalType.LOSE_WEIGHT, null, "Cut 5kg", null, GoalPriority.MEDIUM,
				null, null, null, null, null);
		assertThat(again.id()).isNotEqualTo(replacement.id());
	}

	@Test
	void athleteRowLockSerializesConcurrentDuplicateCreates() throws Exception {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);

		ExecutorService executor = Executors.newFixedThreadPool(2);
		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<Throwable> firstError = new AtomicReference<>();
		AtomicReference<Throwable> secondError = new AtomicReference<>();

		Future<?> first = executor.submit(() -> {
			ready.countDown();
			start.await(5, TimeUnit.SECONDS);
			try {
				createAthleteGoalUseCase.execute(
						accountId, GoalType.IMPROVE_POWER, null, "Power phase", null, GoalPriority.MEDIUM,
						null, null, null, null, null);
			}
			catch (Throwable ex) {
				firstError.set(ex);
			}
			return null;
		});
		Future<?> second = executor.submit(() -> {
			ready.countDown();
			start.await(5, TimeUnit.SECONDS);
			try {
				createAthleteGoalUseCase.execute(
						accountId, GoalType.IMPROVE_POWER, null, "Power phase", null, GoalPriority.HIGH,
						null, null, null, null, null);
			}
			catch (Throwable ex) {
				secondError.set(ex);
			}
			return null;
		});

		assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
		start.countDown();
		first.get(10, TimeUnit.SECONDS);
		second.get(10, TimeUnit.SECONDS);
		executor.shutdownNow();

		long successes = listCurrentAthleteGoalsUseCase.execute(accountId, null, GoalType.IMPROVE_POWER).size();
		assertThat(successes).isEqualTo(1);
		assertThat(firstError.get() == null ^ secondError.get() == null).isTrue();
		Throwable failure = firstError.get() != null ? firstError.get() : secondError.get();
		assertThat(failure).isInstanceOf(DuplicateAthleteGoalException.class);
	}

	@Test
	void supportsStatusLifecycleDeleteAndRejectsInvalidDelete() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteGoalResult goal = createAthleteGoalUseCase.execute(
				accountId, GoalType.IMPROVE_SPEED, null, "Sprint drills", null, GoalPriority.MEDIUM,
				null, null, null, null, null);

		assertThat(changeAthleteGoalStatusUseCase.execute(accountId, goal.id(), GoalStatusAction.PAUSE).status())
				.isEqualTo(GoalStatus.PAUSED);
		assertThatThrownBy(() -> deleteAthleteGoalUseCase.execute(accountId, goal.id()))
				.isInstanceOf(AthleteGoalDeleteRequiresCancelledException.class);

		assertThat(changeAthleteGoalStatusUseCase.execute(accountId, goal.id(), GoalStatusAction.RESUME).status())
				.isEqualTo(GoalStatus.ACTIVE);
		assertThat(changeAthleteGoalStatusUseCase.execute(accountId, goal.id(), GoalStatusAction.COMPLETE).status())
				.isEqualTo(GoalStatus.COMPLETED);
		assertThatThrownBy(() -> updateAthleteGoalUseCase.execute(
				accountId, goal.id(), fullUpdate("Nope", null, GoalPriority.LOW, null, null, null, null, null)))
				.isInstanceOf(TerminalAthleteGoalModificationException.class);
		assertThatThrownBy(() -> deleteAthleteGoalUseCase.execute(accountId, goal.id()))
				.isInstanceOf(AthleteGoalDeleteRequiresCancelledException.class);

		assertThat(changeAthleteGoalStatusUseCase.execute(accountId, goal.id(), GoalStatusAction.REOPEN).status())
				.isEqualTo(GoalStatus.ACTIVE);
		assertThat(changeAthleteGoalStatusUseCase.execute(accountId, goal.id(), GoalStatusAction.CANCEL).status())
				.isEqualTo(GoalStatus.CANCELLED);

		deleteAthleteGoalUseCase.execute(accountId, goal.id());
		assertThatThrownBy(() -> getCurrentAthleteGoalUseCase.execute(accountId, goal.id()))
				.isInstanceOf(AthleteGoalNotFoundException.class);
	}

	@Test
	void rejectsForeignSportArchivedAthleteAndCrossAccountGoalAccess() {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();
		createAthlete(owner);
		createAthlete(other);

		AthleteSportResult otherSport = addAthleteSportUseCase.execute(
				other, SportType.TENNIS, null, true, ParticipationLevel.BEGINNER, null, 1, SeasonStatus.OFF_SEASON);
		assertThatThrownBy(() -> createAthleteGoalUseCase.execute(
				owner, GoalType.IMPROVE_SPORT_PERFORMANCE, null, "Serve speed", null, GoalPriority.MEDIUM,
				null, null, null, null, otherSport.id())).isInstanceOf(AthleteSportNotFoundException.class);
		assertThatThrownBy(() -> createAthleteGoalUseCase.execute(
				owner, GoalType.IMPROVE_SPORT_PERFORMANCE, null, "Serve speed", null, GoalPriority.MEDIUM,
				null, null, null, null, AthleteSportId.generate())).isInstanceOf(AthleteSportNotFoundException.class);

		AthleteGoalResult ownerGoal = createAthleteGoalUseCase.execute(
				owner, GoalType.GENERAL_FITNESS, null, "Stay healthy", null, GoalPriority.LOW,
				null, null, null, null, null);
		assertThatThrownBy(() -> getCurrentAthleteGoalUseCase.execute(other, ownerGoal.id()))
				.isInstanceOf(AthleteGoalNotFoundException.class);

		Athlete athlete = athleteRepository.findByAccountId(owner).orElseThrow();
		athlete.archive(CLOCK);
		athleteRepository.save(athlete);
		assertThatThrownBy(() -> createAthleteGoalUseCase.execute(
				owner, GoalType.MAINTENANCE, null, "Maintain", null, GoalPriority.MEDIUM,
				null, null, null, null, null)).isInstanceOf(AthleteArchivedException.class);
	}

	@Test
	void optimisticLockingAndBigDecimalRoundTrip() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteId athleteId = athleteId(accountId);

		AthleteGoalResult created = createAthleteGoalUseCase.execute(
				accountId,
				GoalType.INCREASE_VERTICAL_JUMP,
				null,
				"Vert jump",
				null,
				GoalPriority.HIGH,
				new BigDecimal("76.250"),
				GoalTargetUnit.CENTIMETER,
				null,
				LocalDate.of(2026, 12, 31),
				null);

		AthleteGoal loaded = athleteGoalRepository.findByIdAndAthleteId(created.id(), athleteId).orElseThrow();
		assertThat(loaded.target()).isEqualTo(GoalTarget.of(new BigDecimal("76.250"), GoalTargetUnit.CENTIMETER));
		assertThat(loaded.version()).isZero();

		loaded.changePriority(GoalPriority.CRITICAL, CLOCK);
		AthleteGoal saved = athleteGoalRepository.save(loaded);
		assertThat(saved.version()).isEqualTo(1L);

		AthleteGoal stale = AthleteGoal.rehydrate(
				loaded.id(),
				loaded.athleteId(),
				loaded.goalType(),
				loaded.customGoalName(),
				loaded.title(),
				loaded.normalizedTitle(),
				loaded.description(),
				GoalPriority.LOW,
				loaded.status(),
				loaded.target(),
				loaded.targetDate(),
				loaded.athleteSportId(),
				loaded.createdAt(),
				loaded.updatedAt(),
				loaded.completedAt(),
				0L);
		assertThatThrownBy(() -> athleteGoalRepository.save(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	private UpdateAthleteGoalCommand fullUpdate(
			String title,
			String description,
			GoalPriority priority,
			BigDecimal targetValue,
			GoalTargetUnit targetUnit,
			String customTargetUnit,
			LocalDate targetDate,
			java.util.UUID athleteSportId) {
		return new UpdateAthleteGoalCommand(
				title, true,
				description, true,
				priority, true,
				targetValue, true,
				targetUnit, true,
				customTargetUnit, true,
				targetDate, true,
				athleteSportId, true);
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

	private AthleteId athleteId(AccountId accountId) {
		return athleteRepository.findByAccountId(accountId).orElseThrow().id();
	}

	@TestConfiguration
	static class FixedClockConfig {

		@Bean
		@Primary
		Clock fixedClock() {
			return CLOCK;
		}

	}

}
