package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * Missing P0/P1 terminal-state edges not already covered by occurrence/set consistency suites.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TerminalLifecycleHardeningIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingEnvironmentUseCase createTrainingEnvironmentUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Autowired
	private SetWorkoutOccurrenceTrainingEnvironmentUseCase setWorkoutOccurrenceTrainingEnvironmentUseCase;

	@Autowired
	private CancelWorkoutOccurrenceUseCase cancelWorkoutOccurrenceUseCase;

	@Autowired
	private SkipWorkoutOccurrenceUseCase skipWorkoutOccurrenceUseCase;

	@Autowired
	private CompleteWorkoutOccurrenceUseCase completeWorkoutOccurrenceUseCase;

	@Autowired
	private CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private CompleteWorkoutExerciseSetUseCase completeWorkoutExerciseSetUseCase;

	@Autowired
	private GenerateWorkoutAdaptationProposalUseCase generateWorkoutAdaptationProposalUseCase;

	@Autowired
	private UpdateWorkoutAdaptationProposalItemUseCase updateWorkoutAdaptationProposalItemUseCase;

	@Autowired
	private CancelWorkoutAdaptationProposalUseCase cancelWorkoutAdaptationProposalUseCase;

	@Autowired
	private ApplyWorkoutAdaptationProposalUseCase applyWorkoutAdaptationProposalUseCase;

	@Test
	void terminalOccurrencesBlockEnvironmentAndAdaptationMutations() {
		Session session = session(LocalDate.of(2026, 7, 6));
		cancelWorkoutOccurrenceUseCase.execute(
				session.accountId(), session.planId(), session.dayId(), session.occurrenceId());

		assertThatThrownBy(() -> setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				session.accountId(),
				session.planId(),
				session.dayId(),
				session.occurrenceId(),
				session.environmentId()))
				.isInstanceOf(WorkoutOccurrenceEnvironmentLockedException.class);
		assertThatThrownBy(() -> generateWorkoutAdaptationProposalUseCase.execute(
				session.accountId(),
				session.planId(),
				session.dayId(),
				session.occurrenceId(),
				3,
				false,
				30))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);

		Session skipped = session(LocalDate.of(2026, 7, 13));
		skipWorkoutOccurrenceUseCase.execute(
				skipped.accountId(), skipped.planId(), skipped.dayId(), skipped.occurrenceId());
		assertThatThrownBy(() -> setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				skipped.accountId(),
				skipped.planId(),
				skipped.dayId(),
				skipped.occurrenceId(),
				skipped.environmentId()))
				.isInstanceOf(WorkoutOccurrenceEnvironmentLockedException.class);
		assertThatThrownBy(() -> generateWorkoutAdaptationProposalUseCase.execute(
				skipped.accountId(),
				skipped.planId(),
				skipped.dayId(),
				skipped.occurrenceId(),
				3,
				false,
				30))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);

		Session completed = session(LocalDate.of(2026, 7, 19));
		completeOccurrence(completed);
		assertThatThrownBy(() -> setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				completed.accountId(),
				completed.planId(),
				completed.dayId(),
				completed.occurrenceId(),
				completed.environmentId()))
				.isInstanceOf(WorkoutOccurrenceEnvironmentLockedException.class);
		assertThatThrownBy(() -> generateWorkoutAdaptationProposalUseCase.execute(
				completed.accountId(),
				completed.planId(),
				completed.dayId(),
				completed.occurrenceId(),
				3,
				false,
				30))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
	}

	@Test
	void cancelledAndAppliedProposalsBlockItemMutation() {
		Session session = session(LocalDate.of(2026, 7, 20));
		WorkoutAdaptationProposalResult proposal = generateWorkoutAdaptationProposalUseCase.execute(
				session.accountId(),
				session.planId(),
				session.dayId(),
				session.occurrenceId(),
				3,
				false,
				30);
		WorkoutAdaptationProposalResult cancelled = cancelWorkoutAdaptationProposalUseCase.execute(
				session.accountId(), proposal.id());
		assertThatThrownBy(() -> updateWorkoutAdaptationProposalItemUseCase.execute(
				session.accountId(),
				cancelled.id(),
				cancelled.items().getFirst().id(),
				WorkoutAdaptationDecision.REJECTED,
				null,
				null,
				"late"))
				.isInstanceOf(WorkoutAdaptationProposalTerminalException.class);

		Session applySession = session(LocalDate.of(2026, 7, 27));
		WorkoutAdaptationProposalResult ready = generateWorkoutAdaptationProposalUseCase.execute(
				applySession.accountId(),
				applySession.planId(),
				applySession.dayId(),
				applySession.occurrenceId(),
				3,
				false,
				30);
		var firstItemId = ready.items().getFirst().id();
		for (WorkoutAdaptationProposalItemResult item : ready.items()) {
			if (item.athleteDecision() == WorkoutAdaptationDecision.PENDING) {
				ready = updateWorkoutAdaptationProposalItemUseCase.execute(
						applySession.accountId(),
						ready.id(),
						item.id(),
						WorkoutAdaptationDecision.ACCEPTED,
						null,
						null,
						null);
			}
		}
		WorkoutAdaptationApplicationResult applied = applyWorkoutAdaptationProposalUseCase.execute(
				applySession.accountId(),
				applySession.planId(),
				applySession.dayId(),
				applySession.occurrenceId(),
				ready.id(),
				ready.version());
		assertThatThrownBy(() -> updateWorkoutAdaptationProposalItemUseCase.execute(
				applySession.accountId(),
				applied.proposalId(),
				firstItemId,
				WorkoutAdaptationDecision.REJECTED,
				null,
				null,
				"after apply"))
				.isInstanceOf(WorkoutAdaptationProposalTerminalException.class);
	}

	private Session session(LocalDate scheduledDate) {
		AccountId accountId = athlete();
		TrainingEnvironmentResult homeGym = createTrainingEnvironmentUseCase.execute(
				accountId,
				"Home " + scheduledDate,
				TrainingEnvironmentType.HOME_GYM,
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH, EquipmentType.OPEN_SPACE),
				null,
				null,
				true);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId,
				TrainingPlanType.STRENGTH,
				null,
				"Terminal " + scheduledDate,
				null,
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 8, 31),
				null,
				null,
				homeGym.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Day", null, 1, DayOfWeek.MONDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				SystemExerciseDefinitions.BACK_SQUAT,
				"Back Squat",
				ExerciseCategory.STRENGTH,
				ExerciseType.BARBELL,
				3,
				5,
				5,
				new BigDecimal("100"),
				WeightUnit.KILOGRAM,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), scheduledDate, null, null);
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, homeGym.id());
		return new Session(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				occurrence.executions().getFirst().id(),
				homeGym.id());
	}

	private void completeOccurrence(Session session) {
		for (WorkoutExerciseSetResult set : listWorkoutExerciseSetsUseCase.execute(
				session.accountId(),
				session.planId(),
				session.dayId(),
				session.occurrenceId(),
				session.executionId())) {
			completeWorkoutExerciseSetUseCase.execute(
					session.accountId(),
					session.planId(),
					session.dayId(),
					session.occurrenceId(),
					session.executionId(),
					set.id());
		}
		completeWorkoutExerciseExecutionUseCase.execute(
				session.accountId(),
				session.planId(),
				session.dayId(),
				session.occurrenceId(),
				session.executionId());
		completeWorkoutOccurrenceUseCase.execute(
				session.accountId(), session.planId(), session.dayId(), session.occurrenceId());
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Sam",
				"River",
				LocalDate.of(1994, 2, 2),
				Sex.MALE,
				Height.ofCentimeters(180),
				Weight.ofKilograms(80),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		return accountId;
	}

	private record Session(
			AccountId accountId,
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId executionId,
			com.devinolabs.uap.training.domain.TrainingEnvironmentId environmentId) {
	}

}
