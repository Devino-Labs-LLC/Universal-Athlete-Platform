package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.ExerciseDefinitionFixtures;
import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanRecurrenceMode;
import com.devinolabs.uap.training.domain.TrainingPlanScheduleStatus;
import com.devinolabs.uap.training.domain.TrainingPlanStatusAction;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceOrigin;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TrainingScheduleUseCaseIntegrationTests {

	/** Wednesday — deliberately not a Monday, to exercise the fixed 7-day window rule. */
	private static final LocalDate START = LocalDate.of(2026, 8, 5);

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private ChangeTrainingPlanStatusUseCase changeTrainingPlanStatusUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private UpdateWorkoutDayUseCase updateWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private ExerciseDefinitionFixtures exerciseDefinitions;

	@Autowired
	private UpdateWorkoutExerciseUseCase updateWorkoutExerciseUseCase;

	@Autowired
	private ActivateTrainingPlanScheduleUseCase activateTrainingPlanScheduleUseCase;

	@Autowired
	private PauseTrainingPlanScheduleUseCase pauseTrainingPlanScheduleUseCase;

	@Autowired
	private ResumeTrainingPlanScheduleUseCase resumeTrainingPlanScheduleUseCase;

	@Autowired
	private CompleteTrainingPlanScheduleUseCase completeTrainingPlanScheduleUseCase;

	@Autowired
	private GenerateWorkoutOccurrencesUseCase generateWorkoutOccurrencesUseCase;

	@Autowired
	private RescheduleWorkoutOccurrenceUseCase rescheduleWorkoutOccurrenceUseCase;

	@Autowired
	private CancelWorkoutOccurrenceUseCase cancelWorkoutOccurrenceUseCase;

	@Autowired
	private StartWorkoutOccurrenceUseCase startWorkoutOccurrenceUseCase;

	@Autowired
	private GetWorkoutOccurrenceUseCase getWorkoutOccurrenceUseCase;

	@Autowired
	private ListWorkoutOccurrencesUseCase listWorkoutOccurrencesUseCase;

	@Autowired
	private GetAthleteTrainingCalendarUseCase getAthleteTrainingCalendarUseCase;

	@Autowired
	private GetAthleteTrainingTodayUseCase getAthleteTrainingTodayUseCase;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Test
	void activatesScheduleAndReportsScheduleFields() {
		Fixture fixture = twoWeekPlan();

		TrainingPlanScheduleActivationResult result = activateTrainingPlanScheduleUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				new ActivateTrainingPlanScheduleCommand(
						START, null, "Europe/Stockholm", TrainingPlanRecurrenceMode.FINITE, null));

		assertThat(result.generation()).isNull();
		TrainingPlanResult plan = result.plan();
		assertThat(plan.scheduleStatus()).isEqualTo(TrainingPlanScheduleStatus.ACTIVE);
		assertThat(plan.scheduleStartDate()).isEqualTo(START);
		assertThat(plan.scheduleTimezone()).isEqualTo("Europe/Stockholm");
		assertThat(plan.recurrenceMode()).isEqualTo(TrainingPlanRecurrenceMode.FINITE);
		assertThat(plan.scheduleActivatedAt()).isNotNull();
		assertThat(plan.scheduleGeneratedThrough()).isNull();
	}

	@Test
	void activationRejectsInvalidTimezoneDatesAndMissingPrerequisites() {
		Fixture fixture = twoWeekPlan();

		assertThatThrownBy(() -> activateTrainingPlanScheduleUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				new ActivateTrainingPlanScheduleCommand(
						START, null, "Not/AZone", TrainingPlanRecurrenceMode.FINITE, null)))
				.isInstanceOf(InvalidTimezoneException.class);

		assertThatThrownBy(() -> activateTrainingPlanScheduleUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				new ActivateTrainingPlanScheduleCommand(
						START, START.minusDays(1), "UTC", TrainingPlanRecurrenceMode.FINITE, null)))
				.isInstanceOf(InvalidTrainingPlanScheduleDatesException.class);

		// FINITE week 2 Friday lands on 2026-08-14, so an earlier end date truncates the plan.
		assertThatThrownBy(() -> activateTrainingPlanScheduleUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				new ActivateTrainingPlanScheduleCommand(
						START, LocalDate.of(2026, 8, 12), "UTC", TrainingPlanRecurrenceMode.FINITE, null)))
				.isInstanceOf(InvalidTrainingPlanScheduleDatesException.class);

		AccountId emptyAccount = AccountId.generate();
		createAthlete(emptyAccount);
		TrainingPlanResult emptyPlan = createTrainingPlanUseCase.execute(
				emptyAccount, TrainingPlanType.STRENGTH, null, "Empty", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);
		assertThatThrownBy(() -> activateTrainingPlanScheduleUseCase.execute(
				emptyAccount,
				emptyPlan.id(),
				new ActivateTrainingPlanScheduleCommand(
						START, null, "UTC", TrainingPlanRecurrenceMode.FINITE, null)))
				.isInstanceOf(TrainingPlanScheduleRequiresWorkoutDaysException.class);

		createWorkoutDayUseCase.execute(
				emptyAccount, emptyPlan.id(), "No Exercises", null, 1, DayOfWeek.MONDAY, null, null, null);
		assertThatThrownBy(() -> activateTrainingPlanScheduleUseCase.execute(
				emptyAccount,
				emptyPlan.id(),
				new ActivateTrainingPlanScheduleCommand(
						START, null, "UTC", TrainingPlanRecurrenceMode.FINITE, null)))
				.isInstanceOf(WorkoutOccurrenceRequiresExercisesException.class);
	}

	@Test
	void activationRejectsArchivedPlansAndDoubleActivation() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);

		assertThatThrownBy(() -> activate(fixture, TrainingPlanRecurrenceMode.FINITE, null))
				.isInstanceOf(InvalidTrainingPlanScheduleStatusException.class);

		Fixture archived = twoWeekPlan();
		changeTrainingPlanStatusUseCase.execute(
				archived.accountId(), archived.planId(), TrainingPlanStatusAction.ARCHIVE);
		assertThatThrownBy(() -> activate(archived, TrainingPlanRecurrenceMode.FINITE, null))
				.isInstanceOf(TrainingPlanArchivedException.class);
	}

	@Test
	void pauseResumeCompleteFollowTheScheduleLifecycle() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);

		assertThat(pauseTrainingPlanScheduleUseCase.execute(fixture.accountId(), fixture.planId()).scheduleStatus())
				.isEqualTo(TrainingPlanScheduleStatus.PAUSED);

		assertThatThrownBy(() -> generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(13)))
				.isInstanceOf(InvalidTrainingPlanScheduleStatusException.class);

		assertThat(resumeTrainingPlanScheduleUseCase.execute(fixture.accountId(), fixture.planId()).scheduleStatus())
				.isEqualTo(TrainingPlanScheduleStatus.ACTIVE);
		assertThat(completeTrainingPlanScheduleUseCase.execute(fixture.accountId(), fixture.planId()).scheduleStatus())
				.isEqualTo(TrainingPlanScheduleStatus.COMPLETED);

		assertThatThrownBy(() -> resumeTrainingPlanScheduleUseCase.execute(fixture.accountId(), fixture.planId()))
				.isInstanceOf(InvalidTrainingPlanScheduleStatusException.class);
	}

	@Test
	void finiteGenerationRunsEachWeekExactlyOnce() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);

		WorkoutOccurrenceGenerationResult generated = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(60));

		// Week 1 Monday = 2026-08-10, week 2 Friday = 2026-08-14; nothing beyond week 2.
		assertThat(generated.createdCount()).isEqualTo(2);
		assertThat(generated.createdOccurrences())
				.extracting(WorkoutOccurrenceResult::scheduledDate)
				.containsExactly(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 14));
		assertThat(generated.createdOccurrences())
				.allMatch(occurrence -> occurrence.origin() == WorkoutOccurrenceOrigin.GENERATED);
		assertThat(generated.scheduleGeneratedThrough()).isEqualTo(LocalDate.of(2026, 8, 14));
	}

	@Test
	void repeatingGenerationCyclesBackToWeekOneAfterTheLastPlanWeek() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.REPEATING, null);

		WorkoutOccurrenceGenerationResult generated = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(55));

		assertThat(generated.createdOccurrences())
				.extracting(WorkoutOccurrenceResult::scheduledDate)
				.containsExactly(
						LocalDate.of(2026, 8, 10),
						LocalDate.of(2026, 8, 14),
						LocalDate.of(2026, 8, 24),
						LocalDate.of(2026, 8, 28),
						LocalDate.of(2026, 9, 7),
						LocalDate.of(2026, 9, 11),
						LocalDate.of(2026, 9, 21),
						LocalDate.of(2026, 9, 25));
		assertThat(generated.scheduleGeneratedThrough()).isEqualTo(START.plusDays(55));
	}

	@Test
	void repeatingGenerationStopsAtScheduleEndDate() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.REPEATING, LocalDate.of(2026, 8, 25));

		WorkoutOccurrenceGenerationResult generated = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(60));

		assertThat(generated.createdOccurrences())
				.extracting(WorkoutOccurrenceResult::scheduledDate)
				.containsExactly(
						LocalDate.of(2026, 8, 10),
						LocalDate.of(2026, 8, 14),
						LocalDate.of(2026, 8, 24));
		assertThat(generated.outOfScheduleCount()).isPositive();
		assertThat(generated.scheduleGeneratedThrough()).isEqualTo(LocalDate.of(2026, 8, 25));
	}

	@Test
	void generationIsIdempotentAndAdvancesTheWatermarkMonotonically() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.REPEATING, null);

		WorkoutOccurrenceGenerationResult first = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(30));
		assertThat(first.createdCount()).isEqualTo(4);

		WorkoutOccurrenceGenerationResult second = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(30));
		assertThat(second.createdCount()).isZero();
		assertThat(second.existingCount()).isEqualTo(4);
		assertThat(second.scheduleGeneratedThrough()).isEqualTo(first.scheduleGeneratedThrough());

		WorkoutOccurrenceGenerationResult backwards = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(10));
		assertThat(backwards.scheduleGeneratedThrough()).isEqualTo(START.plusDays(30));
	}

	@Test
	void cancelledGeneratedOccurrenceActsAsATombstone() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);
		WorkoutOccurrenceGenerationResult generated = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(13));
		WorkoutOccurrenceResult firstOccurrence = generated.createdOccurrences().getFirst();

		cancelWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.weekOneDayId(), firstOccurrence.id());

		WorkoutOccurrenceGenerationResult rerun = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(13));
		assertThat(rerun.createdCount()).isZero();
		assertThat(rerun.cancelledPlacementCount()).isEqualTo(1);
		assertThat(rerun.existingCount()).isEqualTo(1);
	}

	@Test
	void generationSnapshotsAreImmutableWhenThePrescriptionChangesLater() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.REPEATING, null);
		WorkoutOccurrenceGenerationResult first = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(13));
		WorkoutOccurrenceResult original = first.createdOccurrences().getFirst();

		updateWorkoutExerciseUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.weekOneDayId(),
				fixture.weekOneExerciseId(),
				new UpdateWorkoutExerciseCommand(
						null, false,
						"Front Squat", true,
						null, false, null, false,
						9, true,
						null, false, null, false,
						new BigDecimal("140"), true,
						WeightUnit.KILOGRAM, true,
						null, false, null, false, null, false,
						null, false, null, false, null, false, null, false,
						null, false));

		WorkoutOccurrenceGenerationResult later = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START.plusDays(14), START.plusDays(27));

		WorkoutOccurrenceDetailResult before = getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.weekOneDayId(), original.id());
		assertThat(before.executions().getFirst().exerciseName()).isEqualTo("Back Squat");
		assertThat(before.executions().getFirst().prescribedSets()).isEqualTo(3);

		WorkoutOccurrenceDetailResult after = getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.weekOneDayId(),
				later.createdOccurrences().getFirst().id());
		assertThat(after.executions().getFirst().exerciseName()).isEqualTo("Front Squat");
		assertThat(after.executions().getFirst().prescribedSets()).isEqualTo(9);
	}

	@Test
	void generationRangeIsBoundedAndValidated() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);

		assertThatThrownBy(() -> generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), null, START))
				.isInstanceOf(InvalidWorkoutOccurrenceGenerationRangeException.class);
		assertThatThrownBy(() -> generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.minusDays(1)))
				.isInstanceOf(InvalidWorkoutOccurrenceGenerationRangeException.class);
		assertThatThrownBy(() -> generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(90)))
				.isInstanceOf(InvalidWorkoutOccurrenceGenerationRangeException.class);
	}

	@Test
	void generationRequiresAnActiveSchedule() {
		Fixture fixture = twoWeekPlan();
		assertThatThrownBy(() -> generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(13)))
				.isInstanceOf(InvalidTrainingPlanScheduleStatusException.class);
	}

	@Test
	void reschedulingMovesTheOccurrenceAndStopsTheGeneratorRecreatingIt() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);
		WorkoutOccurrenceResult generated = generateWorkoutOccurrencesUseCase
				.execute(fixture.accountId(), fixture.planId(), START, START.plusDays(13))
				.createdOccurrences()
				.getFirst();

		WorkoutOccurrenceDetailResult moved = rescheduleWorkoutOccurrenceUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.weekOneDayId(),
				generated.id(),
				LocalDate.of(2026, 8, 11),
				LocalTime.of(18, 0));

		assertThat(moved.occurrence().scheduledDate()).isEqualTo(LocalDate.of(2026, 8, 11));
		assertThat(moved.occurrence().plannedStartTime()).isEqualTo(LocalTime.of(18, 0));
		assertThat(moved.occurrence().manuallyRescheduled()).isTrue();
		assertThat(moved.occurrence().originalScheduledDate()).isEqualTo(LocalDate.of(2026, 8, 10));

		WorkoutOccurrenceGenerationResult rerun = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(13));
		assertThat(rerun.createdCount()).isZero();
		assertThat(rerun.existingCount()).isEqualTo(2);
	}

	@Test
	void reschedulingIsRejectedOnceWorkHasStarted() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);
		WorkoutOccurrenceResult generated = generateWorkoutOccurrencesUseCase
				.execute(fixture.accountId(), fixture.planId(), START, START.plusDays(13))
				.createdOccurrences()
				.getFirst();

		startWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.weekOneDayId(), generated.id());

		assertThatThrownBy(() -> rescheduleWorkoutOccurrenceUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.weekOneDayId(),
				generated.id(),
				LocalDate.of(2026, 8, 11),
				null))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
	}

	@Test
	void placementChangesAreLockedOnceOccurrencesExistForTheDay() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);
		generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(13));

		assertThatThrownBy(() -> updateWorkoutDayUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.weekOneDayId(),
				new UpdateWorkoutDayCommand(
						null, false,
						null, false,
						null, false,
						DayOfWeek.SUNDAY, true,
						null, false,
						null, false,
						null, false)))
				.isInstanceOf(TrainingPlanSchedulePlacementLockedException.class);

		WorkoutDayResult renamed = updateWorkoutDayUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.weekOneDayId(),
				new UpdateWorkoutDayCommand(
						"Renamed Only", true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false));
		assertThat(renamed.title()).isEqualTo("Renamed Only");
	}

	@Test
	void duplicatePlacementsAreRejected() {
		Fixture fixture = twoWeekPlan();

		assertThatThrownBy(() -> createWorkoutDayUseCase.execute(
				fixture.accountId(), fixture.planId(), "Clashing", null, 1, DayOfWeek.MONDAY, null, null, null))
				.isInstanceOf(DuplicateWorkoutDayPlacementException.class);

		WorkoutDayResult differentTime = createWorkoutDayUseCase.execute(
				fixture.accountId(), fixture.planId(), "Evening", null, 1, DayOfWeek.MONDAY,
				LocalTime.of(18, 0), null, null);
		assertThat(differentTime.plannedStartTime()).isEqualTo(LocalTime.of(18, 0));
	}

	@Test
	void calendarReturnsOrderedEntriesWithLabelsAndExecutionCounts() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);
		generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), START, START.plusDays(13));

		List<AthleteCalendarEntryResult> entries = getAthleteTrainingCalendarUseCase.execute(
				fixture.accountId(), START, START.plusDays(13), null, null);

		assertThat(entries).hasSize(2);
		assertThat(entries).extracting(AthleteCalendarEntryResult::scheduledDate)
				.containsExactly(LocalDate.of(2026, 8, 10), LocalDate.of(2026, 8, 14));
		AthleteCalendarEntryResult first = entries.getFirst();
		assertThat(first.trainingPlanName()).isEqualTo("Strength");
		assertThat(first.workoutDayName()).isEqualTo("Week One");
		assertThat(first.exerciseCount()).isEqualTo(1);
		assertThat(first.notStartedExerciseCount()).isEqualTo(1);
		assertThat(first.completedExerciseCount()).isZero();
		assertThat(first.origin()).isEqualTo(WorkoutOccurrenceOrigin.GENERATED);
	}

	@Test
	void calendarFiltersByStatusAndPlanAndBoundsTheRange() {
		Fixture fixture = twoWeekPlan();
		activate(fixture, TrainingPlanRecurrenceMode.FINITE, null);
		WorkoutOccurrenceResult generated = generateWorkoutOccurrencesUseCase
				.execute(fixture.accountId(), fixture.planId(), START, START.plusDays(13))
				.createdOccurrences()
				.getFirst();
		startWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.weekOneDayId(), generated.id());

		assertThat(getAthleteTrainingCalendarUseCase.execute(
				fixture.accountId(), START, START.plusDays(13), WorkoutOccurrenceStatus.IN_PROGRESS, null))
				.hasSize(1);
		assertThat(getAthleteTrainingCalendarUseCase.execute(
				fixture.accountId(), START, START.plusDays(13), WorkoutOccurrenceStatus.COMPLETED, null))
				.isEmpty();
		assertThat(getAthleteTrainingCalendarUseCase.execute(
				fixture.accountId(), START, START.plusDays(13), null, fixture.planId()))
				.hasSize(2);

		assertThatThrownBy(() -> getAthleteTrainingCalendarUseCase.execute(
				fixture.accountId(), START, START.plusYears(3), null, null))
				.isInstanceOf(InvalidTrainingCalendarRangeException.class);
		assertThatThrownBy(() -> getAthleteTrainingCalendarUseCase.execute(
				fixture.accountId(), START, START.minusDays(1), null, null))
				.isInstanceOf(InvalidTrainingCalendarRangeException.class);
	}

	@Test
	void todayRequiresAValidTimezoneAndResolvesTheLocalDate() {
		Fixture fixture = twoWeekPlan();

		assertThatThrownBy(() -> getAthleteTrainingTodayUseCase.execute(fixture.accountId(), null))
				.isInstanceOf(InvalidTimezoneException.class);
		assertThatThrownBy(() -> getAthleteTrainingTodayUseCase.execute(fixture.accountId(), "Mars/Olympus"))
				.isInstanceOf(InvalidTimezoneException.class);

		AthleteTrainingTodayResult stockholm = getAthleteTrainingTodayUseCase.execute(
				fixture.accountId(), "Europe/Stockholm");
		AthleteTrainingTodayResult auckland = getAthleteTrainingTodayUseCase.execute(
				fixture.accountId(), "Pacific/Auckland");

		assertThat(stockholm.timezone()).isEqualTo("Europe/Stockholm");
		assertThat(stockholm.date()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Europe/Stockholm")));
		assertThat(auckland.date()).isEqualTo(LocalDate.now(java.time.ZoneId.of("Pacific/Auckland")));
	}

	@Test
	void todayListsOccurrencesScheduledForTheResolvedLocalDate() {
		Fixture fixture = twoWeekPlan();
		LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Stockholm"));
		createWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.weekOneDayId(), today, LocalTime.of(7, 0), null);

		AthleteTrainingTodayResult result = getAthleteTrainingTodayUseCase.execute(
				fixture.accountId(), "Europe/Stockholm");

		assertThat(result.entries()).hasSize(1);
		assertThat(result.entries().getFirst().scheduledDate()).isEqualTo(today);
		assertThat(result.entries().getFirst().origin()).isEqualTo(WorkoutOccurrenceOrigin.MANUAL);
	}

	@Test
	void manuallyCreatedOccurrencesAreMarkedManual() {
		Fixture fixture = twoWeekPlan();
		WorkoutOccurrenceDetailResult manual = createWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.weekOneDayId(),
				LocalDate.of(2026, 9, 30), null, null);

		assertThat(manual.occurrence().origin()).isEqualTo(WorkoutOccurrenceOrigin.MANUAL);
		assertThat(manual.occurrence().manuallyRescheduled()).isFalse();
	}

	@Test
	void criticalAcceptanceFiniteVerticalProgramSnapshotsCancelAndReschedule() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "8-Week Vertical Program", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);

		WorkoutDayResult weekOne = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower Body Strength", null, 1, DayOfWeek.MONDAY, null, 60, null);
		WorkoutExerciseResult weekOneSquat = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), weekOne.id(), exerciseDefinitions.idFor(accountId, "Back Squat"),
				"Back Squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				5, 5, 5, new BigDecimal("225"), WeightUnit.POUND,
				null, null, null, null, null, null, null, null);

		WorkoutDayResult weekTwo = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower Body Strength Progression", null, 2, DayOfWeek.MONDAY, null, 60,
				null);
		WorkoutExerciseResult weekTwoSquat = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), weekTwo.id(), exerciseDefinitions.idFor(accountId, "Back Squat"),
				"Back Squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				5, 5, 5, new BigDecimal("225"), WeightUnit.POUND,
				null, null, null, null, null, null, null, null);

		activateTrainingPlanScheduleUseCase.execute(
				accountId,
				plan.id(),
				new ActivateTrainingPlanScheduleCommand(
						START, null, "America/New_York", TrainingPlanRecurrenceMode.FINITE, null));

		// First horizon covers only week 1 (Wed 2026-08-05 .. Tue 2026-08-11 → Monday 2026-08-10).
		WorkoutOccurrenceGenerationResult weekOneGeneration = generateWorkoutOccurrencesUseCase.execute(
				accountId, plan.id(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 11));
		assertThat(weekOneGeneration.createdCount()).isEqualTo(1);
		assertThat(weekOneGeneration.createdOccurrences().getFirst().scheduledDate())
				.isEqualTo(LocalDate.of(2026, 8, 10));
		WorkoutOccurrenceDetailResult weekOneDetail = getWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), weekOne.id(), weekOneGeneration.createdOccurrences().getFirst().id());
		assertThat(weekOneDetail.executions().getFirst().prescribedSets()).isEqualTo(5);
		assertThat(weekOneDetail.executions().getFirst().prescribedMinimumReps()).isEqualTo(5);
		assertThat(weekOneDetail.executions().getFirst().prescribedMaximumReps()).isEqualTo(5);
		assertThat(weekOneDetail.executions().getFirst().prescribedTargetWeight()).isEqualByComparingTo("225");

		WorkoutOccurrenceGenerationResult idempotent = generateWorkoutOccurrencesUseCase.execute(
				accountId, plan.id(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 11));
		assertThat(idempotent.createdCount()).isZero();
		assertThat(idempotent.existingCount()).isEqualTo(1);

		cancelWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), weekOne.id(), weekOneGeneration.createdOccurrences().getFirst().id());
		WorkoutOccurrenceGenerationResult afterCancel = generateWorkoutOccurrencesUseCase.execute(
				accountId, plan.id(), LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 11));
		assertThat(afterCancel.createdCount()).isZero();
		assertThat(afterCancel.cancelledPlacementCount()).isEqualTo(1);

		updateWorkoutExerciseUseCase.execute(
				accountId,
				plan.id(),
				weekTwo.id(),
				weekTwoSquat.id(),
				new UpdateWorkoutExerciseCommand(
						null, false,
						null, false, null, false, null, false,
						4, true, 8, true, 8, true,
						new BigDecimal("185"), true, WeightUnit.POUND, true,
						null, false, null, false, null, false,
						null, false, null, false, null, false, null, false,
						null, false));

		WorkoutOccurrenceGenerationResult weekTwoGeneration = generateWorkoutOccurrencesUseCase.execute(
				accountId, plan.id(), LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 31));
		assertThat(weekTwoGeneration.createdCount()).isEqualTo(1);
		assertThat(weekTwoGeneration.createdOccurrences().getFirst().scheduledDate())
				.isEqualTo(LocalDate.of(2026, 8, 17));
		WorkoutOccurrenceDetailResult weekTwoDetail = getWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), weekTwo.id(), weekTwoGeneration.createdOccurrences().getFirst().id());
		assertThat(weekTwoDetail.executions().getFirst().prescribedSets()).isEqualTo(4);
		assertThat(weekTwoDetail.executions().getFirst().prescribedMinimumReps()).isEqualTo(8);
		assertThat(weekTwoDetail.executions().getFirst().prescribedMaximumReps()).isEqualTo(8);
		assertThat(weekTwoDetail.executions().getFirst().prescribedTargetWeight()).isEqualByComparingTo("185");

		// Earlier cancelled week-1 snapshot remains the original prescription.
		assertThat(weekOneDetail.executions().getFirst().prescribedSets()).isEqualTo(5);
		assertThat(weekOneDetail.executions().getFirst().prescribedTargetWeight()).isEqualByComparingTo("225");
		assertThat(weekOneSquat.sets()).isEqualTo(5);

		WorkoutOccurrenceDetailResult rescheduled = rescheduleWorkoutOccurrenceUseCase.execute(
				accountId,
				plan.id(),
				weekTwo.id(),
				weekTwoGeneration.createdOccurrences().getFirst().id(),
				LocalDate.of(2026, 8, 18),
				LocalTime.of(18, 30));
		assertThat(rescheduled.occurrence().scheduledDate()).isEqualTo(LocalDate.of(2026, 8, 18));
		assertThat(rescheduled.occurrence().plannedStartTime()).isEqualTo(LocalTime.of(18, 30));
		assertThat(rescheduled.occurrence().manuallyRescheduled()).isTrue();
		assertThat(rescheduled.occurrence().originalScheduledDate()).isEqualTo(LocalDate.of(2026, 8, 17));
		assertThat(rescheduled.executions().getFirst().prescribedSets()).isEqualTo(4);
		assertThat(rescheduled.executions().getFirst().prescribedTargetWeight()).isEqualByComparingTo("185");

		WorkoutOccurrenceGenerationResult afterReschedule = generateWorkoutOccurrencesUseCase.execute(
				accountId, plan.id(), LocalDate.of(2026, 8, 12), LocalDate.of(2026, 8, 31));
		assertThat(afterReschedule.createdCount()).isZero();
		assertThat(afterReschedule.existingCount()).isEqualTo(1);
	}

	@Test
	void acceptanceScenarioTwoWeekRepeatingPlanFromActivationToCalendar() {
		Fixture fixture = twoWeekPlan();

		TrainingPlanScheduleActivationResult activation = activateTrainingPlanScheduleUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				new ActivateTrainingPlanScheduleCommand(
						START, null, "Europe/Stockholm", TrainingPlanRecurrenceMode.REPEATING,
						LocalDate.of(2026, 9, 1)));

		assertThat(activation.plan().scheduleStatus()).isEqualTo(TrainingPlanScheduleStatus.ACTIVE);
		assertThat(activation.generation()).isNotNull();
		assertThat(activation.generation().createdOccurrences())
				.extracting(WorkoutOccurrenceResult::scheduledDate)
				.containsExactly(
						LocalDate.of(2026, 8, 10),
						LocalDate.of(2026, 8, 14),
						LocalDate.of(2026, 8, 24),
						LocalDate.of(2026, 8, 28));
		assertThat(activation.plan().scheduleGeneratedThrough()).isEqualTo(LocalDate.of(2026, 9, 1));

		WorkoutOccurrenceResult firstMonday = activation.generation().createdOccurrences().getFirst();
		rescheduleWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.weekOneDayId(), firstMonday.id(),
				LocalDate.of(2026, 8, 11), LocalTime.of(19, 30));

		WorkoutOccurrenceGenerationResult extended = generateWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), LocalDate.of(2026, 9, 2), LocalDate.of(2026, 9, 30));
		assertThat(extended.createdCount()).isEqualTo(4);
		assertThat(extended.scheduleGeneratedThrough()).isEqualTo(LocalDate.of(2026, 9, 30));

		List<AthleteCalendarEntryResult> calendar = getAthleteTrainingCalendarUseCase.execute(
				fixture.accountId(), START, LocalDate.of(2026, 9, 30), null, fixture.planId());
		assertThat(calendar).hasSize(8);
		assertThat(calendar).extracting(AthleteCalendarEntryResult::scheduledDate).isSorted();
		assertThat(calendar.getFirst().scheduledDate()).isEqualTo(LocalDate.of(2026, 8, 11));
		assertThat(calendar.getFirst().manuallyRescheduled()).isTrue();
		assertThat(calendar.getFirst().originalScheduledDate()).isEqualTo(LocalDate.of(2026, 8, 10));
		assertThat(calendar).allMatch(entry -> entry.exerciseCount() == 1);

		assertThat(listWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.weekOneDayId(), null, null, null))
				.hasSize(4);
	}

	private TrainingPlanResult activate(
			Fixture fixture,
			TrainingPlanRecurrenceMode mode,
			LocalDate scheduleEndDate) {
		return activateTrainingPlanScheduleUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				new ActivateTrainingPlanScheduleCommand(START, scheduleEndDate, "Europe/Stockholm", mode, null))
				.plan();
	}

	/**
	 * Two-week plan: week 1 Monday and week 2 Friday, each with one exercise.
	 * With a Wednesday schedule start those land on 2026-08-10 and 2026-08-14.
	 */
	private Fixture twoWeekPlan() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);

		WorkoutDayResult weekOne = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Week One", null, 1, DayOfWeek.MONDAY, null, 60, null);
		WorkoutExerciseResult weekOneExercise = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), weekOne.id(), exerciseDefinitions.idFor(accountId, "Back Squat"),
				"Back Squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);

		WorkoutDayResult weekTwo = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Week Two", null, 2, DayOfWeek.FRIDAY, null, 60, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), weekTwo.id(), exerciseDefinitions.idFor(accountId, "Bench Press"),
				"Bench Press", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 8, 8, new BigDecimal("80"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);

		return new Fixture(accountId, plan.id(), weekOne.id(), weekOneExercise.id(), weekTwo.id());
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

	private record Fixture(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId weekOneDayId,
			com.devinolabs.uap.training.domain.WorkoutExerciseId weekOneExerciseId,
			WorkoutDayId weekTwoDayId) {
	}

}
