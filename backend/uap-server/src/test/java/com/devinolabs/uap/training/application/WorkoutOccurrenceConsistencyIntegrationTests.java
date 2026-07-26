package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.support.TransactionTemplate;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.application.AthleteRepository;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutOccurrenceConsistencyIntegrationTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-25T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Autowired
	private UpdateWorkoutOccurrenceUseCase updateWorkoutOccurrenceUseCase;

	@Autowired
	private GetWorkoutOccurrenceUseCase getWorkoutOccurrenceUseCase;

	@Autowired
	private ListWorkoutOccurrencesUseCase listWorkoutOccurrencesUseCase;

	@Autowired
	private StartWorkoutOccurrenceUseCase startWorkoutOccurrenceUseCase;

	@Autowired
	private CompleteWorkoutOccurrenceUseCase completeWorkoutOccurrenceUseCase;

	@Autowired
	private SkipWorkoutOccurrenceUseCase skipWorkoutOccurrenceUseCase;

	@Autowired
	private CancelWorkoutOccurrenceUseCase cancelWorkoutOccurrenceUseCase;

	@Autowired
	private DeleteWorkoutOccurrenceUseCase deleteWorkoutOccurrenceUseCase;

	@Autowired
	private StartWorkoutExerciseExecutionUseCase startWorkoutExerciseExecutionUseCase;

	@Autowired
	private UpdateWorkoutExerciseExecutionUseCase updateWorkoutExerciseExecutionUseCase;

	@Autowired
	private CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;

	@Autowired
	private SkipWorkoutExerciseExecutionUseCase skipWorkoutExerciseExecutionUseCase;

	@Autowired
	private AthleteRepository athleteRepository;

	@Autowired
	private AthleteContextPort athleteContextPort;

	@Autowired
	private TrainingPlanRepository trainingPlanRepository;

	@Autowired
	private WorkoutDayRepository workoutDayRepository;

	@Autowired
	private WorkoutExerciseRepository workoutExerciseRepository;

	@Autowired
	private WorkoutOccurrenceRepository workoutOccurrenceRepository;

	@Autowired
	private WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

	@Autowired
	private Clock clock;

	@Test
	void skipScheduledOccurrencePropagatesToNotStartedChildren() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 10));
		Instant before = fixture.occurrence().executions().getFirst().updatedAt();

		WorkoutOccurrenceDetailResult skipped = skipWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());

		assertThat(skipped.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.SKIPPED);
		assertThat(skipped.occurrence().completedAt()).isNull();
		assertThat(skipped.executions()).hasSize(1);
		assertThat(skipped.executions().getFirst().status()).isEqualTo(WorkoutExerciseExecutionStatus.SKIPPED);
		assertThat(skipped.executions().getFirst().completedAt()).isNull();
		assertThat(skipped.executions().getFirst().updatedAt()).isAfterOrEqualTo(before);
	}

	@Test
	void skipInProgressOccurrencePropagatesToOpenChildren() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 11));
		startWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());

		WorkoutOccurrenceDetailResult skipped = skipWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());

		assertThat(skipped.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.SKIPPED);
		assertThat(skipped.executions().getFirst().status()).isEqualTo(WorkoutExerciseExecutionStatus.SKIPPED);
		assertThat(skipped.executions().getFirst().completedAt()).isNull();
		assertThat(skipped.executions().getFirst().updatedAt()).isNotNull();
	}

	@Test
	void skipOccurrencePreservesCompletedAndSkippedChildrenAndSkipsOpenOnes() {
		Fixture fixture = fixtureWithExercises(3, LocalDate.of(2026, 8, 12));
		List<WorkoutExerciseExecutionResult> executions = fixture.occurrence().executions();
		WorkoutExerciseExecutionResult first = executions.get(0);
		WorkoutExerciseExecutionResult second = executions.get(1);
		WorkoutExerciseExecutionResult third = executions.get(2);

		startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), first.id());
		completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), first.id());
		skipWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), second.id());
		startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), third.id());

		WorkoutOccurrenceDetailResult beforeSkip = getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		Instant completedUpdatedAt = byId(beforeSkip, first.id()).updatedAt();
		Instant alreadySkippedUpdatedAt = byId(beforeSkip, second.id()).updatedAt();
		Instant inProgressUpdatedAt = byId(beforeSkip, third.id()).updatedAt();

		WorkoutOccurrenceDetailResult skipped = skipWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());

		assertThat(skipped.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.SKIPPED);
		assertThat(byId(skipped, first.id()).status()).isEqualTo(WorkoutExerciseExecutionStatus.COMPLETED);
		assertThat(byId(skipped, first.id()).completedAt()).isNotNull();
		assertThat(byId(skipped, first.id()).updatedAt()).isEqualTo(completedUpdatedAt);
		assertThat(byId(skipped, second.id()).status()).isEqualTo(WorkoutExerciseExecutionStatus.SKIPPED);
		assertThat(byId(skipped, second.id()).updatedAt()).isEqualTo(alreadySkippedUpdatedAt);
		assertThat(byId(skipped, third.id()).status()).isEqualTo(WorkoutExerciseExecutionStatus.SKIPPED);
		assertThat(byId(skipped, third.id()).completedAt()).isNull();
		assertThat(byId(skipped, third.id()).updatedAt()).isAfterOrEqualTo(inProgressUpdatedAt);
		assertThat(skipped.executions())
				.extracting(WorkoutExerciseExecutionResult::status)
				.doesNotContain(
						WorkoutExerciseExecutionStatus.NOT_STARTED,
						WorkoutExerciseExecutionStatus.IN_PROGRESS);
	}

	@Test
	void skipOccurrenceRollsBackWhenChildPersistenceFails() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 13));
		WorkoutExerciseExecutionRepository failing = failingSaveAllRepository();
		SkipWorkoutOccurrenceUseCase failingSkip = new SkipWorkoutOccurrenceUseCase(
				athleteContextPort,
				trainingPlanRepository,
				workoutDayRepository,
				workoutOccurrenceRepository,
				failing,
				clock);

		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> failingSkip.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId())))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("child saveAll failed");

		WorkoutOccurrenceDetailResult reloaded = getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		assertThat(reloaded.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.SCHEDULED);
		assertThat(reloaded.executions().getFirst().status()).isEqualTo(WorkoutExerciseExecutionStatus.NOT_STARTED);
	}

	@Test
	void cancelUntouchedScheduledOccurrenceKeepsReadableSnapshots() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 14));

		WorkoutOccurrenceDetailResult cancelled = cancelWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());

		assertThat(cancelled.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.CANCELLED);
		assertThat(cancelled.occurrence().startedAt()).isNull();
		assertThat(cancelled.occurrence().completedAt()).isNull();
		assertThat(cancelled.executions()).hasSize(1);
		assertThat(cancelled.executions().getFirst().status()).isEqualTo(WorkoutExerciseExecutionStatus.NOT_STARTED);
		assertThat(cancelled.executions().getFirst().startedAt()).isNull();
		assertThat(cancelled.executions().getFirst().completedAt()).isNull();
		assertThat(cancelled.executions().getFirst().prescribedSets()).isEqualTo(3);
	}

	@Test
	void cancelRejectedAfterExecutionStarts() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 15));
		startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				fixture.occurrenceId(),
				fixture.occurrence().executions().getFirst().id());

		assertThatThrownBy(() -> cancelWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId()))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);

		WorkoutOccurrenceDetailResult reloaded = getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		assertThat(reloaded.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.IN_PROGRESS);
	}

	@Test
	void executionMutationRejectedAfterCancellation() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 16));
		WorkoutExerciseExecutionId executionId = fixture.occurrence().executions().getFirst().id();
		cancelWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());

		assertThatThrownBy(() -> startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), executionId))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> updateWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				fixture.occurrenceId(),
				executionId,
				new UpdateWorkoutExerciseExecutionCommand(
						3, true, 8, true, null, false, null, false, null, false, null, false, null, false, null, false,
						null, false, null, false)))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), executionId))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> skipWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), executionId))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);

		WorkoutOccurrenceDetailResult reloaded = getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		assertThat(reloaded.executions().getFirst().startedAt()).isNull();
		assertThat(reloaded.executions().getFirst().completedAt()).isNull();
		assertThat(reloaded.executions().getFirst().status()).isEqualTo(WorkoutExerciseExecutionStatus.NOT_STARTED);
	}

	@Test
	void startingExecutionFromScheduledParentAtomicallyPromotesOccurrence() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 17));
		WorkoutExerciseExecutionId executionId = fixture.occurrence().executions().getFirst().id();
		assertThat(fixture.occurrence().occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.SCHEDULED);

		WorkoutExerciseExecutionResult started = startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), executionId);

		WorkoutOccurrenceDetailResult detail = getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		assertThat(started.status()).isEqualTo(WorkoutExerciseExecutionStatus.IN_PROGRESS);
		assertThat(detail.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.IN_PROGRESS);
		assertThat(detail.occurrence().startedAt()).isNotNull();
	}

	@Test
	void terminalParentsRejectExecutionStartUpdateCompleteAndSkip() {
		assertTerminalParentRejectsMutations(terminalCompletedFixture());
		assertTerminalParentRejectsMutations(terminalSkippedFixture());
		assertTerminalParentRejectsMutations(terminalCancelledFixture());
	}

	@Test
	void completionRequiresTerminalChildrenSetsCompletedAtOnceAndBlocksFurtherMutation() {
		Fixture fixture = fixtureWithExercises(2, LocalDate.of(2026, 8, 18));
		WorkoutExerciseExecutionId first = fixture.occurrence().executions().get(0).id();
		WorkoutExerciseExecutionId second = fixture.occurrence().executions().get(1).id();

		startWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		assertThatThrownBy(() -> completeWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId()))
				.isInstanceOf(WorkoutOccurrenceHasIncompleteExercisesException.class);

		startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), first);
		completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), first);
		skipWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), second);

		WorkoutOccurrenceDetailResult completed = completeWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		assertThat(completed.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.COMPLETED);
		assertThat(completed.occurrence().completedAt()).isNotNull();
		Instant completedAt = completed.occurrence().completedAt();

		WorkoutOccurrenceDetailResult again = completeWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		assertThat(again.occurrence().completedAt()).isEqualTo(completedAt);

		assertThatThrownBy(() -> startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), first))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> updateWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				fixture.occurrenceId(),
				first,
				new UpdateWorkoutExerciseExecutionCommand(
						3, true, null, false, null, false, null, false, null, false, null, false, null, false, null,
						false, null, false, null, false)))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> skipWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId()))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> cancelWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId()))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> deleteWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId()))
				.isInstanceOf(WorkoutOccurrenceDeleteNotAllowedException.class);
		assertThatThrownBy(() -> startWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId()))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> updateWorkoutOccurrenceUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				fixture.occurrenceId(),
				new UpdateWorkoutOccurrenceCommand(
						LocalDate.of(2026, 9, 1), true, null, false, null, false)))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
	}

	@Test
	void createAndUpdateDuplicateScheduledDateRaceMapsToDuplicateWorkoutOccurrence() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 19));
		AthleteId athleteId = athleteId(fixture.accountId());
		LocalDate conflictingDate = LocalDate.of(2026, 8, 19);

		WorkoutOccurrence collidingCreate = WorkoutOccurrence.create(
				com.devinolabs.uap.training.domain.WorkoutOccurrenceId.generate(),
				fixture.planId(),
				fixture.dayId(),
				athleteId,
				conflictingDate,
				null,
				null,
				CLOCK);
		assertThatThrownBy(() -> workoutOccurrenceRepository.save(collidingCreate))
				.isInstanceOf(DuplicateWorkoutOccurrenceException.class)
				.isNotInstanceOf(DataIntegrityViolationException.class);

		WorkoutOccurrenceDetailResult other = createWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), LocalDate.of(2026, 8, 20), null, null);
		WorkoutOccurrence loaded = workoutOccurrenceRepository
				.findByIdAndWorkoutDayIdAndAthleteId(other.occurrence().id(), fixture.dayId(), athleteId)
				.orElseThrow();
		loaded.changeScheduledDate(conflictingDate, CLOCK);
		assertThatThrownBy(() -> workoutOccurrenceRepository.save(loaded))
				.isInstanceOf(DuplicateWorkoutOccurrenceException.class)
				.isNotInstanceOf(DataIntegrityViolationException.class);

		assertThatThrownBy(() -> updateWorkoutOccurrenceUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				other.occurrence().id(),
				new UpdateWorkoutOccurrenceCommand(conflictingDate, true, null, false, null, false)))
				.isInstanceOf(DuplicateWorkoutOccurrenceException.class);
	}

	@Test
	void createOccurrenceRollsBackWhenExecutionSnapshotPersistenceFails() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Day", null, DayOfWeek.MONDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(),
				"Squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);

		CreateWorkoutOccurrenceUseCase failingCreate = new CreateWorkoutOccurrenceUseCase(
				athleteContextPort,
				trainingPlanRepository,
				workoutDayRepository,
				workoutExerciseRepository,
				workoutOccurrenceRepository,
				failingSaveAllRepository(),
				clock);

		LocalDate scheduledDate = LocalDate.of(2026, 8, 21);
		assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> failingCreate.execute(
				accountId, plan.id(), day.id(), scheduledDate, null, null)))
				.isInstanceOf(IllegalStateException.class)
				.hasMessage("child saveAll failed");

		assertThat(listWorkoutOccurrencesUseCase.execute(accountId, plan.id(), day.id(), null, null, null))
				.isEmpty();
		assertThat(workoutExerciseExecutionRepository.findAllByWorkoutOccurrenceIdAndAthleteId(
				com.devinolabs.uap.training.domain.WorkoutOccurrenceId.generate(),
				athleteId(accountId))).isEmpty();
		assertThat(workoutOccurrenceRepository.existsByWorkoutDayIdAndAthleteIdAndScheduledDateAndStatusNot(
				day.id(), athleteId(accountId), scheduledDate, WorkoutOccurrenceStatus.CANCELLED)).isFalse();
	}

	@Test
	void optimisticLockingRejectsStaleOccurrenceAndExecutionUpdates() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 22));
		AthleteId athleteId = athleteId(fixture.accountId());

		WorkoutOccurrence loaded = workoutOccurrenceRepository
				.findByIdAndWorkoutDayIdAndAthleteId(fixture.occurrenceId(), fixture.dayId(), athleteId)
				.orElseThrow();
		assertThat(loaded.version()).isZero();
		loaded.updateDetails(null, "first write", CLOCK);
		WorkoutOccurrence saved = workoutOccurrenceRepository.save(loaded);
		assertThat(saved.version()).isEqualTo(1L);

		WorkoutOccurrence staleOccurrence = WorkoutOccurrence.rehydrate(
				loaded.id(),
				loaded.trainingPlanId(),
				loaded.workoutDayId(),
				loaded.athleteId(),
				loaded.scheduledDate(),
				loaded.plannedStartTime(),
				loaded.startedAt(),
				loaded.completedAt(),
				loaded.status(),
				"stale",
				loaded.createdAt(),
				loaded.updatedAt(),
				0L);
		assertThatThrownBy(() -> workoutOccurrenceRepository.save(staleOccurrence))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		WorkoutExerciseExecution execution = workoutExerciseExecutionRepository
				.findAllByWorkoutOccurrenceIdAndAthleteId(fixture.occurrenceId(), athleteId)
				.getFirst();
		assertThat(execution.version()).isZero();
		execution.updateNotes("first", CLOCK);
		WorkoutExerciseExecution savedExecution = workoutExerciseExecutionRepository.save(execution);
		assertThat(savedExecution.version()).isEqualTo(1L);

		WorkoutExerciseExecution staleExecution = WorkoutExerciseExecution.rehydrate(
				execution.id(),
				execution.workoutOccurrenceId(),
				execution.sourceWorkoutExerciseId(),
				execution.athleteId(),
				execution.displayOrder(),
				execution.exerciseName(),
				execution.category(),
				execution.type(),
				execution.prescribedSets(),
				execution.prescribedMinimumReps(),
				execution.prescribedMaximumReps(),
				execution.prescribedTargetWeight(),
				execution.prescribedWeightUnit(),
				execution.prescribedTargetDurationSeconds(),
				execution.prescribedTargetDistance(),
				execution.prescribedDistanceUnit(),
				execution.prescribedTargetRestSeconds(),
				execution.prescribedTargetRpe(),
				execution.prescribedTempo(),
				execution.prescribedCoachingNotes(),
				execution.status(),
				execution.actualSets(),
				execution.actualReps(),
				execution.actualWeight(),
				execution.weightUnit(),
				execution.actualDurationSeconds(),
				execution.actualDistance(),
				execution.distanceUnit(),
				execution.actualRestSeconds(),
				execution.actualRpe(),
				execution.startedAt(),
				execution.completedAt(),
				"stale",
				execution.createdAt(),
				execution.updatedAt(),
				0L);
		assertThatThrownBy(() -> workoutExerciseExecutionRepository.save(staleExecution))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	@Test
	void deleteRequiresScheduledUntouchedChildrenAndCascadesSnapshots() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 23));
		AthleteId athleteId = athleteId(fixture.accountId());
		WorkoutExerciseExecutionId executionId = fixture.occurrence().executions().getFirst().id();

		deleteWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		assertThat(listWorkoutOccurrencesUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), null, null, null)).isEmpty();
		assertThat(workoutExerciseExecutionRepository.findAllByWorkoutOccurrenceIdAndAthleteId(
				fixture.occurrenceId(), athleteId)).isEmpty();
		assertThat(workoutExerciseExecutionRepository.findByIdAndWorkoutOccurrenceIdAndAthleteId(
				executionId, fixture.occurrenceId(), athleteId)).isEmpty();

		Fixture blocked = fixtureWithExercises(1, LocalDate.of(2026, 8, 24));
		WorkoutExerciseExecution untouched = workoutExerciseExecutionRepository
				.findAllByWorkoutOccurrenceIdAndAthleteId(blocked.occurrenceId(), athleteId(blocked.accountId()))
				.getFirst();
		untouched.start(CLOCK);
		workoutExerciseExecutionRepository.save(untouched);

		assertThat(getWorkoutOccurrenceUseCase.execute(
				blocked.accountId(), blocked.planId(), blocked.dayId(), blocked.occurrenceId())
				.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.SCHEDULED);
		assertThatThrownBy(() -> deleteWorkoutOccurrenceUseCase.execute(
				blocked.accountId(), blocked.planId(), blocked.dayId(), blocked.occurrenceId()))
				.isInstanceOf(WorkoutOccurrenceDeleteNotAllowedException.class);
		assertThat(workoutExerciseExecutionRepository.findAllByWorkoutOccurrenceIdAndAthleteId(
				blocked.occurrenceId(), athleteId(blocked.accountId()))).hasSize(1);
	}

	private void assertTerminalParentRejectsMutations(Fixture fixture) {
		WorkoutExerciseExecutionId executionId = fixture.occurrence().executions().getFirst().id();
		assertThatThrownBy(() -> startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), executionId))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> updateWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				fixture.occurrenceId(),
				executionId,
				new UpdateWorkoutExerciseExecutionCommand(
						1, true, null, false, null, false, null, false, null, false, null, false, null, false, null,
						false, null, false, null, false)))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), executionId))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThatThrownBy(() -> skipWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), executionId))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
	}

	private Fixture terminalCompletedFixture() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 25));
		WorkoutExerciseExecutionId executionId = fixture.occurrence().executions().getFirst().id();
		startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), executionId);
		completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(), executionId);
		completeWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		return reload(fixture);
	}

	private Fixture terminalSkippedFixture() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 26));
		skipWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		return reload(fixture);
	}

	private Fixture terminalCancelledFixture() {
		Fixture fixture = fixtureWithExercises(1, LocalDate.of(2026, 8, 27));
		cancelWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		return reload(fixture);
	}

	private Fixture reload(Fixture fixture) {
		WorkoutOccurrenceDetailResult detail = getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		return new Fixture(fixture.accountId(), fixture.planId(), fixture.dayId(), detail);
	}

	private Fixture fixtureWithExercises(int exerciseCount, LocalDate scheduledDate) {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 8, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Day-" + scheduledDate, null, DayOfWeek.MONDAY, null, null, null);
		for (int i = 0; i < exerciseCount; i++) {
			createWorkoutExerciseUseCase.execute(
					accountId, plan.id(), day.id(),
					"Exercise " + i, ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
					3, 8, 8, new BigDecimal("100"), WeightUnit.KILOGRAM,
					null, null, null, null, null, null, null, null);
		}
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), scheduledDate, null, null);
		return new Fixture(accountId, plan.id(), day.id(), occurrence);
	}

	private WorkoutExerciseExecutionRepository failingSaveAllRepository() {
		WorkoutExerciseExecutionRepository delegate = workoutExerciseExecutionRepository;
		return new WorkoutExerciseExecutionRepository() {
			@Override
			public WorkoutExerciseExecution save(WorkoutExerciseExecution execution) {
				return delegate.save(execution);
			}

			@Override
			public List<WorkoutExerciseExecution> saveAll(Collection<WorkoutExerciseExecution> executions) {
				throw new IllegalStateException("child saveAll failed");
			}

			@Override
			public Optional<WorkoutExerciseExecution> findByIdAndWorkoutOccurrenceIdAndAthleteId(
					WorkoutExerciseExecutionId id,
					com.devinolabs.uap.training.domain.WorkoutOccurrenceId occurrenceId,
					AthleteId athleteId) {
				return delegate.findByIdAndWorkoutOccurrenceIdAndAthleteId(id, occurrenceId, athleteId);
			}

			@Override
			public List<WorkoutExerciseExecution> findAllByWorkoutOccurrenceIdAndAthleteId(
					com.devinolabs.uap.training.domain.WorkoutOccurrenceId occurrenceId,
					AthleteId athleteId) {
				return delegate.findAllByWorkoutOccurrenceIdAndAthleteId(occurrenceId, athleteId);
			}

			@Override
			public Optional<WorkoutExerciseExecution> findByIdAndWorkoutDayIdAndAthleteId(
					WorkoutExerciseExecutionId id,
					com.devinolabs.uap.training.domain.WorkoutDayId dayId,
					com.devinolabs.uap.training.domain.WorkoutOccurrenceId occurrenceId,
					AthleteId athleteId) {
				return delegate.findByIdAndWorkoutDayIdAndAthleteId(id, dayId, occurrenceId, athleteId);
			}
		};
	}

	private static WorkoutExerciseExecutionResult byId(
			WorkoutOccurrenceDetailResult detail,
			WorkoutExerciseExecutionId id) {
		return detail.executions().stream()
				.filter(execution -> execution.id().equals(id))
				.findFirst()
				.orElseThrow();
	}

	private AthleteId athleteId(AccountId accountId) {
		return AthleteId.of(athleteRepository
				.findByAccountId(com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()))
				.orElseThrow()
				.id()
				.value());
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
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			WorkoutOccurrenceDetailResult occurrence) {

		com.devinolabs.uap.training.domain.WorkoutOccurrenceId occurrenceId() {
			return occurrence.occurrence().id();
		}
	}

}
