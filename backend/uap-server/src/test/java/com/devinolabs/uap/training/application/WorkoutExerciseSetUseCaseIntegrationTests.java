package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.ExerciseDefinitionFixtures;
import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.AthleteRepository;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetType;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutExerciseSetUseCaseIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private ExerciseDefinitionFixtures exerciseDefinitions;

	@Autowired
	private UpdateWorkoutExerciseUseCase updateWorkoutExerciseUseCase;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Autowired
	private GetWorkoutOccurrenceUseCase getWorkoutOccurrenceUseCase;

	@Autowired
	private SkipWorkoutOccurrenceUseCase skipWorkoutOccurrenceUseCase;

	@Autowired
	private CancelWorkoutOccurrenceUseCase cancelWorkoutOccurrenceUseCase;

	@Autowired
	private SkipWorkoutExerciseExecutionUseCase skipWorkoutExerciseExecutionUseCase;

	@Autowired
	private CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;

	@Autowired
	private GetWorkoutExerciseExecutionUseCase getWorkoutExerciseExecutionUseCase;

	@Autowired
	private StartWorkoutExerciseExecutionUseCase startWorkoutExerciseExecutionUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private GetWorkoutExerciseSetUseCase getWorkoutExerciseSetUseCase;

	@Autowired
	private AddWorkoutExerciseSetUseCase addWorkoutExerciseSetUseCase;

	@Autowired
	private UpdateWorkoutExerciseSetUseCase updateWorkoutExerciseSetUseCase;

	@Autowired
	private StartWorkoutExerciseSetUseCase startWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseSetUseCase completeWorkoutExerciseSetUseCase;

	@Autowired
	private SkipWorkoutExerciseSetUseCase skipWorkoutExerciseSetUseCase;

	@Autowired
	private DeleteWorkoutExerciseSetUseCase deleteWorkoutExerciseSetUseCase;

	@Autowired
	private ReorderWorkoutExerciseSetsUseCase reorderWorkoutExerciseSetsUseCase;

	@Autowired
	private WorkoutExerciseSetRepository workoutExerciseSetRepository;

	@Autowired
	private AthleteRepository athleteRepository;

	@Test
	void creatingAnOccurrenceSeedsOneSetPerPrescribedSet() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 1), 3);

		List<WorkoutExerciseSetResult> sets = listSets(fixture);

		assertThat(sets).hasSize(3);
		assertThat(sets).extracting(WorkoutExerciseSetResult::setNumber).containsExactly(1, 2, 3);
		assertThat(sets).extracting(WorkoutExerciseSetResult::displayOrder).containsExactly(0, 1, 2);
		assertThat(sets).allSatisfy(set -> {
			assertThat(set.setType()).isEqualTo(WorkoutExerciseSetType.WORKING);
			assertThat(set.status()).isEqualTo(WorkoutExerciseSetStatus.NOT_STARTED);
			assertThat(set.prescribedMinimumReps()).isEqualTo(8);
			assertThat(set.prescribedWeight()).isEqualByComparingTo("100");
			assertThat(set.prescribedWeightUnit()).isEqualTo(WeightUnit.KILOGRAM);
			assertThat(set.workoutExerciseExecutionId()).isEqualTo(fixture.executionId());
		});
		assertThat(getWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), sets.getFirst().id()).id())
				.isEqualTo(sets.getFirst().id());
	}

	@Test
	void executionWithoutPrescribedSetCountStillGetsOneSet() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 2), 1);

		assertThat(listSets(fixture)).hasSize(1);
	}

	@Test
	void loggingActualsPromotesOccurrenceExecutionAndSet() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 3), 3);
		WorkoutExerciseSetResult first = listSets(fixture).getFirst();
		assertThat(fixture.occurrence().occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.SCHEDULED);

		WorkoutExerciseSetResult logged = updateWorkoutExerciseSetUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				fixture.occurrenceId(),
				fixture.executionId(),
				first.id(),
				actuals(8, new BigDecimal("100"), WeightUnit.KILOGRAM, 90, new BigDecimal("7.50")));

		assertThat(logged.status()).isEqualTo(WorkoutExerciseSetStatus.IN_PROGRESS);
		assertThat(logged.actualReps()).isEqualTo(8);
		assertThat(logged.startedAt()).isNotNull();
		assertThat(logged.completedAt()).isNull();

		WorkoutOccurrenceDetailResult detail = getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId());
		assertThat(detail.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.IN_PROGRESS);
		assertThat(detail.executions().getFirst().status()).isEqualTo(WorkoutExerciseExecutionStatus.IN_PROGRESS);
		assertThat(detail.executions().getFirst().setCounts().setCount()).isEqualTo(3);
		assertThat(detail.executions().getFirst().setCounts().inProgressSetCount()).isEqualTo(1);
		assertThat(detail.executions().getFirst().setCounts().notStartedSetCount()).isEqualTo(2);
	}

	@Test
	void startAndSkipAlsoPromoteParents() {
		Fixture started = fixture(LocalDate.of(2026, 9, 4), 2);
		startWorkoutExerciseSetUseCase.execute(
				started.accountId(), started.planId(), started.dayId(), started.occurrenceId(),
				started.executionId(), listSets(started).getFirst().id());
		assertThat(occurrenceStatus(started)).isEqualTo(WorkoutOccurrenceStatus.IN_PROGRESS);

		Fixture skipped = fixture(LocalDate.of(2026, 9, 5), 2);
		WorkoutExerciseSetResult set = skipWorkoutExerciseSetUseCase.execute(
				skipped.accountId(), skipped.planId(), skipped.dayId(), skipped.occurrenceId(),
				skipped.executionId(), listSets(skipped).getFirst().id());
		assertThat(set.status()).isEqualTo(WorkoutExerciseSetStatus.SKIPPED);
		assertThat(occurrenceStatus(skipped)).isEqualTo(WorkoutOccurrenceStatus.IN_PROGRESS);
	}

	@Test
	void completingAnExecutionDerivesSummaryAggregatesFromCompletedSets() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 6), 3);
		List<WorkoutExerciseSetResult> sets = listSets(fixture);

		logAndComplete(fixture, sets.get(0), 8, new BigDecimal("100"), 120, new BigDecimal("7"));
		logAndComplete(fixture, sets.get(1), 6, new BigDecimal("100"), 130, new BigDecimal("8"));
		skipWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), sets.get(2).id());

		WorkoutExerciseExecutionResult completed = completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId());

		assertThat(completed.status()).isEqualTo(WorkoutExerciseExecutionStatus.COMPLETED);
		assertThat(completed.actualSets()).isEqualTo(2);
		assertThat(completed.actualReps()).isEqualTo(14);
		assertThat(completed.actualWeight()).isEqualByComparingTo("100");
		assertThat(completed.weightUnit()).isEqualTo(WeightUnit.KILOGRAM);
		assertThat(completed.actualRestSeconds()).isEqualTo(125);
		assertThat(completed.actualRpe()).isEqualByComparingTo("7.50");
		assertThat(completed.setCounts().completedSetCount()).isEqualTo(2);
		assertThat(completed.setCounts().skippedSetCount()).isEqualTo(1);
	}

	@Test
	void mixedCompletedWeightsCollapseToNullSummaryWeight() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 7), 2);
		List<WorkoutExerciseSetResult> sets = listSets(fixture);
		logAndComplete(fixture, sets.get(0), 5, new BigDecimal("100"), null, null);
		logAndComplete(fixture, sets.get(1), 5, new BigDecimal("110"), null, null);

		WorkoutExerciseExecutionResult completed = completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId());

		assertThat(completed.actualWeight()).isNull();
		assertThat(completed.weightUnit()).isNull();
		assertThat(completed.actualReps()).isEqualTo(10);
		assertThat(completed.actualRestSeconds()).isNull();
		assertThat(completed.actualRpe()).isNull();
	}

	@Test
	void completingAnExecutionWithActiveSetsIsRejected() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 8), 2);
		List<WorkoutExerciseSetResult> sets = listSets(fixture);
		logAndComplete(fixture, sets.get(0), 8, new BigDecimal("100"), null, null);

		assertThatThrownBy(() -> completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId()))
				.isInstanceOf(WorkoutExerciseExecutionHasIncompleteSetsException.class);
	}

	@Test
	void mixedDistanceUnitsAcrossCompletedSetsAreRejected() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 9), 2);
		List<WorkoutExerciseSetResult> sets = listSets(fixture);
		logDistanceAndComplete(fixture, sets.get(0), new BigDecimal("400"), DistanceUnit.METER);
		logDistanceAndComplete(fixture, sets.get(1), new BigDecimal("1"), DistanceUnit.MILE);

		assertThatThrownBy(() -> completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId()))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("distance units");
	}

	@Test
	void skippingAnExecutionOrOccurrenceSkipsEveryActiveSet() {
		Fixture execution = fixture(LocalDate.of(2026, 9, 10), 3);
		startWorkoutExerciseSetUseCase.execute(
				execution.accountId(), execution.planId(), execution.dayId(), execution.occurrenceId(),
				execution.executionId(), listSets(execution).getFirst().id());

		skipWorkoutExerciseExecutionUseCase.execute(
				execution.accountId(), execution.planId(), execution.dayId(), execution.occurrenceId(),
				execution.executionId());
		assertThat(listSets(execution))
				.extracting(WorkoutExerciseSetResult::status)
				.containsOnly(WorkoutExerciseSetStatus.SKIPPED);

		Fixture occurrence = fixture(LocalDate.of(2026, 9, 11), 3);
		skipWorkoutOccurrenceUseCase.execute(
				occurrence.accountId(), occurrence.planId(), occurrence.dayId(), occurrence.occurrenceId());
		assertThat(workoutExerciseSetRepository.findAllByOccurrenceIdAndAthleteId(
				occurrence.occurrenceId(), athleteId(occurrence.accountId())))
				.extracting(WorkoutExerciseSet::status)
				.containsOnly(WorkoutExerciseSetStatus.SKIPPED);
	}

	@Test
	void terminalParentsRejectSetWrites() {
		Fixture cancelled = fixture(LocalDate.of(2026, 9, 12), 2);
		WorkoutExerciseSetId cancelledSetId = listSets(cancelled).getFirst().id();
		cancelWorkoutOccurrenceUseCase.execute(
				cancelled.accountId(), cancelled.planId(), cancelled.dayId(), cancelled.occurrenceId());
		assertThatThrownBy(() -> startWorkoutExerciseSetUseCase.execute(
				cancelled.accountId(), cancelled.planId(), cancelled.dayId(), cancelled.occurrenceId(),
				cancelled.executionId(), cancelledSetId))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
		assertThat(listSets(cancelled)).hasSize(2);

		Fixture skippedExecution = fixture(LocalDate.of(2026, 9, 13), 2);
		WorkoutExerciseSetId skippedSetId = listSets(skippedExecution).getFirst().id();
		skipWorkoutExerciseExecutionUseCase.execute(
				skippedExecution.accountId(), skippedExecution.planId(), skippedExecution.dayId(),
				skippedExecution.occurrenceId(), skippedExecution.executionId());
		assertThatThrownBy(() -> completeWorkoutExerciseSetUseCase.execute(
				skippedExecution.accountId(), skippedExecution.planId(), skippedExecution.dayId(),
				skippedExecution.occurrenceId(), skippedExecution.executionId(), skippedSetId))
				.isInstanceOf(InvalidWorkoutExerciseExecutionStatusException.class);
	}

	@Test
	void addSetCopiesPrescriptionAndEnforcesTheHundredSetCeiling() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 14), 2);

		WorkoutExerciseSetResult fromExecution = addWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(),
				new AddWorkoutExerciseSetCommand(null, null, null, null, null, null, null, null, null, null, null));
		assertThat(fromExecution.setNumber()).isEqualTo(3);
		assertThat(fromExecution.displayOrder()).isEqualTo(2);
		assertThat(fromExecution.setType()).isEqualTo(WorkoutExerciseSetType.WORKING);
		assertThat(fromExecution.prescribedWeight()).isEqualByComparingTo("100");

		WorkoutExerciseSetResult explicit = addWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(),
				new AddWorkoutExerciseSetCommand(
						null, WorkoutExerciseSetType.DROP_SET, 3, 5, new BigDecimal("60"), WeightUnit.KILOGRAM,
						null, null, null, 6, 45));
		assertThat(explicit.setType()).isEqualTo(WorkoutExerciseSetType.DROP_SET);
		assertThat(explicit.prescribedWeight()).isEqualByComparingTo("60");
		assertThat(explicit.prescribedRestSeconds()).isEqualTo(45);

		WorkoutExerciseSetResult copied = addWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(),
				new AddWorkoutExerciseSetCommand(
						explicit.id(), null, null, null, null, null, null, null, null, null, null));
		assertThat(copied.setType()).isEqualTo(WorkoutExerciseSetType.DROP_SET);
		assertThat(copied.prescribedWeight()).isEqualByComparingTo("60");
		assertThat(copied.actualReps()).isNull();
		assertThat(copied.status()).isEqualTo(WorkoutExerciseSetStatus.NOT_STARTED);

		while (listSets(fixture).size() < 100) {
			addWorkoutExerciseSetUseCase.execute(
					fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
					fixture.executionId(),
					new AddWorkoutExerciseSetCommand(
							null, null, null, null, null, null, null, null, null, null, null));
		}
		assertThatThrownBy(() -> addWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(),
				new AddWorkoutExerciseSetCommand(null, null, null, null, null, null, null, null, null, null, null)))
				.isInstanceOf(WorkoutExerciseSetLimitExceededException.class);
	}

	@Test
	void deleteRemovesOnlyUntouchedSetsAndKeepsTheSequenceDense() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 15), 3);
		List<WorkoutExerciseSetResult> sets = listSets(fixture);

		deleteWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), sets.get(1).id());

		List<WorkoutExerciseSetResult> remaining = listSets(fixture);
		assertThat(remaining).hasSize(2);
		assertThat(remaining).extracting(WorkoutExerciseSetResult::setNumber).containsExactly(1, 2);
		assertThat(remaining).extracting(WorkoutExerciseSetResult::displayOrder).containsExactly(0, 1);
		assertThat(remaining).extracting(WorkoutExerciseSetResult::id)
				.containsExactly(sets.get(0).id(), sets.get(2).id());

		startWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), remaining.getFirst().id());
		assertThatThrownBy(() -> deleteWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), remaining.getFirst().id()))
				.isInstanceOf(WorkoutExerciseSetDeleteNotAllowedException.class);

		Fixture single = fixture(LocalDate.of(2026, 9, 20), 1);
		WorkoutExerciseSetId lastSetId = listSets(single).getFirst().id();
		assertThatThrownBy(() -> deleteWorkoutExerciseSetUseCase.execute(
				single.accountId(), single.planId(), single.dayId(), single.occurrenceId(),
				single.executionId(), lastSetId))
				.isInstanceOf(WorkoutExerciseExecutionRequiresSetException.class);
	}

	@Test
	void reorderRewritesTheSequenceAndValidatesMembership() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 16), 3);
		List<WorkoutExerciseSetResult> sets = listSets(fixture);
		List<UUID> reversed = List.of(
				sets.get(2).id().value(), sets.get(1).id().value(), sets.get(0).id().value());

		List<WorkoutExerciseSetResult> reordered = reorderWorkoutExerciseSetsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), reversed);

		assertThat(reordered).extracting(WorkoutExerciseSetResult::id)
				.containsExactly(sets.get(2).id(), sets.get(1).id(), sets.get(0).id());
		assertThat(reordered).extracting(WorkoutExerciseSetResult::setNumber).containsExactly(1, 2, 3);
		assertThat(reordered).extracting(WorkoutExerciseSetResult::displayOrder).containsExactly(0, 1, 2);

		assertThatThrownBy(() -> reorderWorkoutExerciseSetsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), List.of(sets.get(0).id().value())))
				.isInstanceOf(InvalidWorkoutExerciseSetMembershipException.class);
		assertThatThrownBy(() -> reorderWorkoutExerciseSetsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(),
				List.of(sets.get(0).id().value(), sets.get(0).id().value(), sets.get(1).id().value())))
				.isInstanceOf(InvalidWorkoutExerciseSetMembershipException.class);
		assertThatThrownBy(() -> reorderWorkoutExerciseSetsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(),
				List.of(sets.get(0).id().value(), sets.get(1).id().value(), UUID.randomUUID())))
				.isInstanceOf(InvalidWorkoutExerciseSetMembershipException.class);

		startWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), reordered.getFirst().id());
		assertThatThrownBy(() -> reorderWorkoutExerciseSetsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), reversed))
				.isInstanceOf(WorkoutExerciseSetReorderNotAllowedException.class);
	}

	@Test
	void setsFromAnotherExecutionAreNotVisible() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 17), 2);
		createWorkoutExerciseUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(),
				exerciseDefinitions.idFor(fixture.accountId(), "Second Exercise"),
				"Second Exercise", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				2, 8, 8, new BigDecimal("50"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);
		WorkoutOccurrenceDetailResult other = createWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), LocalDate.of(2026, 9, 18), null, null);
		WorkoutExerciseExecutionId otherExecutionId = other.executions().getLast().id();
		WorkoutExerciseSetId foreignSetId = listWorkoutExerciseSetsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), other.occurrence().id(), otherExecutionId)
				.getFirst()
				.id();

		assertThatThrownBy(() -> getWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), foreignSetId))
				.isInstanceOf(WorkoutExerciseSetNotFoundException.class);
	}

	@Test
	void legacyExecutionsWithoutSetsStayReadableAndCompletable() {
		Fixture fixture = fixture(LocalDate.of(2026, 9, 19), 2);
		AthleteId athleteId = athleteId(fixture.accountId());
		for (WorkoutExerciseSet set : workoutExerciseSetRepository.findAllByExecutionIdAndAthleteId(
				fixture.executionId(), athleteId)) {
			workoutExerciseSetRepository.delete(set);
		}

		assertThat(listSets(fixture)).isEmpty();
		assertThat(getWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId()).setCounts().setCount()).isZero();

		startWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId());
		WorkoutExerciseExecutionResult completed = completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId());
		assertThat(completed.status()).isEqualTo(WorkoutExerciseExecutionStatus.COMPLETED);
		assertThat(completed.actualSets()).isNull();
		assertThat(completed.actualReps()).isNull();
	}

	@Test
	void backSquatAcceptanceScenarioDerivesSummaryAndLaterPrescriptionChangePreservesHistory() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Squat Day", null, 1, DayOfWeek.MONDAY, null, null, null);
		WorkoutExerciseResult squat = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), exerciseDefinitions.idFor(accountId, "Back Squat"),
				"Back Squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				5, 5, 5, new BigDecimal("225"), WeightUnit.POUND,
				null, null, null, 180, 8, null, null, null);

		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), LocalDate.of(2026, 9, 21), null, null);
		WorkoutExerciseExecutionId executionId = occurrence.executions().getFirst().id();
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		List<WorkoutExerciseSetResult> sets = listWorkoutExerciseSetsUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, executionId);
		assertThat(sets).hasSize(5);

		BigDecimal[] weights = {
			new BigDecimal("225"), new BigDecimal("225"), new BigDecimal("235"),
			new BigDecimal("235"), new BigDecimal("245") };
		int[] rests = { 180, 190, 200, 210, 210 };
		String[] rpes = { "7.5", "8", "8", "8.5", "8.5" };
		for (int i = 0; i < sets.size(); i++) {
			updateWorkoutExerciseSetUseCase.execute(
					accountId, plan.id(), day.id(), occurrenceId, executionId, sets.get(i).id(),
					actuals(5, weights[i], WeightUnit.POUND, rests[i], new BigDecimal(rpes[i])));
			completeWorkoutExerciseSetUseCase.execute(
					accountId, plan.id(), day.id(), occurrenceId, executionId, sets.get(i).id());
		}

		WorkoutExerciseExecutionResult completed = completeWorkoutExerciseExecutionUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, executionId);
		assertThat(completed.actualSets()).isEqualTo(5);
		assertThat(completed.actualReps()).isEqualTo(25);
		assertThat(completed.actualWeight()).isNull();
		assertThat(completed.weightUnit()).isNull();
		assertThat(completed.actualRestSeconds()).isEqualTo(198);
		assertThat(completed.actualRpe()).isEqualByComparingTo("8.10");
		assertThat(completed.actualRpe().scale()).isEqualTo(2);

		updateWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), squat.id(),
				new UpdateWorkoutExerciseCommand(
						null, false,
						null, false,
						null, false,
						null, false,
						4, true,
						8, true,
						8, true,
						new BigDecimal("185"), true,
						WeightUnit.POUND, true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false));

		WorkoutOccurrenceDetailResult next = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), LocalDate.of(2026, 9, 28), null, null);
		List<WorkoutExerciseSetResult> nextSets = listWorkoutExerciseSetsUseCase.execute(
				accountId, plan.id(), day.id(), next.occurrence().id(), next.executions().getFirst().id());
		assertThat(nextSets).hasSize(4);
		assertThat(nextSets).allSatisfy(set -> {
			assertThat(set.prescribedMinimumReps()).isEqualTo(8);
			assertThat(set.prescribedWeight()).isEqualByComparingTo("185");
			assertThat(set.status()).isEqualTo(WorkoutExerciseSetStatus.NOT_STARTED);
		});

		List<WorkoutExerciseSetResult> historical = listWorkoutExerciseSetsUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, executionId);
		assertThat(historical).hasSize(5);
		assertThat(historical).allSatisfy(set -> {
			assertThat(set.prescribedWeight()).isEqualByComparingTo("225");
			assertThat(set.status()).isEqualTo(WorkoutExerciseSetStatus.COMPLETED);
		});
		assertThat(historical).extracting(WorkoutExerciseSetResult::actualWeight)
				.usingElementComparator(BigDecimal::compareTo)
				.containsExactly(weights);
	}

	private WorkoutOccurrenceStatus occurrenceStatus(Fixture fixture) {
		return getWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId())
				.occurrence()
				.status();
	}

	private List<WorkoutExerciseSetResult> listSets(Fixture fixture) {
		return listWorkoutExerciseSetsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId());
	}

	private void logAndComplete(
			Fixture fixture,
			WorkoutExerciseSetResult set,
			Integer reps,
			BigDecimal weight,
			Integer restSeconds,
			BigDecimal rpe) {
		updateWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), set.id(),
				actuals(reps, weight, weight == null ? null : WeightUnit.KILOGRAM, restSeconds, rpe));
		completeWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), set.id());
	}

	private void logDistanceAndComplete(
			Fixture fixture,
			WorkoutExerciseSetResult set,
			BigDecimal distance,
			DistanceUnit unit) {
		updateWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), set.id(),
				new UpdateWorkoutExerciseSetCommand(
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						distance, true,
						unit, true,
						null, false,
						null, false,
						null, false));
		completeWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), fixture.occurrenceId(),
				fixture.executionId(), set.id());
	}

	private static UpdateWorkoutExerciseSetCommand actuals(
			Integer reps,
			BigDecimal weight,
			WeightUnit weightUnit,
			Integer restSeconds,
			BigDecimal rpe) {
		return new UpdateWorkoutExerciseSetCommand(
				null, false,
				reps, true,
				weight, true,
				weightUnit, true,
				null, false,
				null, false,
				null, false,
				restSeconds, restSeconds != null,
				rpe, rpe != null,
				null, false);
	}

	private Fixture fixture(LocalDate scheduledDate, int prescribedSets) {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Strength", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Day-" + scheduledDate, null, 1, DayOfWeek.MONDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), exerciseDefinitions.idFor(accountId, "Squat"),
				"Squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				prescribedSets, 8, 8, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), scheduledDate, null, null);
		return new Fixture(accountId, plan.id(), day.id(), occurrence);
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
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceDetailResult occurrence) {

		WorkoutOccurrenceId occurrenceId() {
			return occurrence.occurrence().id();
		}

		WorkoutExerciseExecutionId executionId() {
			return occurrence.executions().getFirst().id();
		}
	}

}
