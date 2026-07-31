package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.ExerciseDefinitionMetadataFixtures;
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
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionMetadata;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.KineticChainType;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class WorkoutAdaptationProposalAcceptanceIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingEnvironmentUseCase createTrainingEnvironmentUseCase;

	@Autowired
	private CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;

	@Autowired
	private CreateExerciseSubstitutionRelationshipUseCase createExerciseSubstitutionRelationshipUseCase;

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
	private GenerateWorkoutAdaptationProposalUseCase generateWorkoutAdaptationProposalUseCase;

	@Autowired
	private UpdateWorkoutAdaptationProposalItemUseCase updateWorkoutAdaptationProposalItemUseCase;

	@Autowired
	private ApplyWorkoutAdaptationProposalUseCase applyWorkoutAdaptationProposalUseCase;

	@Autowired
	private AnalyzeWorkoutOccurrenceFeasibilityUseCase analyzeWorkoutOccurrenceFeasibilityUseCase;

	@Autowired
	private ListWorkoutExerciseSubstitutionHistoryUseCase listWorkoutExerciseSubstitutionHistoryUseCase;

	@Autowired
	private StartWorkoutExerciseSetUseCase startWorkoutExerciseSetUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Test
	void criticalHomeGymScenarioCoversGenerateReviewApplyAndStaleness() {
		AccountId accountId = athlete();
		TrainingEnvironmentResult homeGym = createTrainingEnvironmentUseCase.execute(
				accountId,
				"Home Gym",
				TrainingEnvironmentType.HOME_GYM,
				List.of(
						EquipmentType.DUMBBELL,
						EquipmentType.BENCH,
						EquipmentType.RESISTANCE_BAND,
						EquipmentType.PULL_UP_BAR,
						EquipmentType.OPEN_SPACE),
				null,
				null,
				true);
		ExerciseDefinitionId pullUpId = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Pull-Up", ExerciseDefinitionMetadataFixtures.pullUp()).id();
		ExerciseDefinitionId dumbbellBenchPressId = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Dumbbell Bench Press", ExerciseDefinitionMetadataFixtures.dumbbellBenchPress()).id();
		ExerciseDefinitionId floorPressId = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Floor Press", floorPress()).id();
		var benchToDbRelationship = createExerciseSubstitutionRelationshipUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BENCH_PRESS,
				dumbbellBenchPressId,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				ExerciseSubstitutionCompatibility.HIGH,
				"Dumbbells at home");
		var benchToFloorRelationship = createExerciseSubstitutionRelationshipUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BENCH_PRESS,
				floorPressId,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				ExerciseSubstitutionCompatibility.MODERATE,
				"Floor press option");

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId,
				TrainingPlanType.STRENGTH,
				null,
				"Adaptation Block",
				null,
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 8, 31),
				null,
				null,
				homeGym.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId,
				plan.id(),
				"Full Body",
				null,
				1,
				DayOfWeek.MONDAY,
				null,
				null,
				null);
		prescribe(accountId, plan.id(), day.id(), SystemExerciseDefinitions.BACK_SQUAT, "Back Squat", ExerciseType.BARBELL);
		prescribe(accountId, plan.id(), day.id(), SystemExerciseDefinitions.BENCH_PRESS, "Bench Press", ExerciseType.BARBELL);
		prescribe(accountId, plan.id(), day.id(), pullUpId, "Pull-Up", ExerciseType.BODYWEIGHT);
		prescribe(accountId, plan.id(), day.id(), SystemExerciseDefinitions.PLANK, "Plank", ExerciseType.BODYWEIGHT);

		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), LocalDate.of(2026, 6, 8), null, null);
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, homeGym.id());

		WorkoutOccurrenceFeasibilityResult currentFeasibility = analyzeWorkoutOccurrenceFeasibilityUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, 3, false);
		assertThat(currentFeasibility.summary().status()).isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);
		assertThat(currentFeasibility.summary().feasibilityPercentage()).isEqualByComparingTo(new BigDecimal("50.00"));

		WorkoutAdaptationProposalResult proposal = generateWorkoutAdaptationProposalUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, 3, true, 30);
		assertThat(proposal.status())
				.as("item actions: %s", proposal.items().stream()
						.map(item -> item.prescribedNameSnapshot() + "=" + item.action())
						.toList())
				.isEqualTo(WorkoutAdaptationProposalStatus.READY);
		assertThat(proposal.totalExecutions()).isEqualTo(4);
		assertThat(proposal.alreadyFeasibleExecutions()).isEqualTo(2);
		assertThat(proposal.proposedSubstitutions()).isEqualTo(2);
		assertThat(proposal.unresolvedExecutions()).isZero();
		assertThat(proposal.expectedFeasibleExecutions()).isEqualTo(4);
		assertThat(proposal.expectedFeasibilityPercentage()).isEqualByComparingTo(new BigDecimal("100.00"));

		WorkoutAdaptationProposalItemResult backSquatItem = proposal.items().get(0);
		WorkoutAdaptationProposalItemResult benchPressItem = proposal.items().get(1);
		updateWorkoutAdaptationProposalItemUseCase.execute(
				accountId,
				proposal.id(),
				backSquatItem.id(),
				WorkoutAdaptationDecision.ACCEPTED,
				null,
				null,
				null);
		WorkoutAdaptationProposalResult reviewed = updateWorkoutAdaptationProposalItemUseCase.execute(
				accountId,
				proposal.id(),
				benchPressItem.id(),
				WorkoutAdaptationDecision.OVERRIDDEN,
				floorPressId,
				benchToFloorRelationship.id(),
				"Use floor press today");

		WorkoutAdaptationApplicationResult application = applyWorkoutAdaptationProposalUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				reviewed.id(),
				reviewed.version());
		assertThat(application.proposalStatus()).isEqualTo(WorkoutAdaptationProposalStatus.APPLIED);
		assertThat(application.substitutionsApplied()).isEqualTo(2);
		assertThat(application.finalWorkoutFeasibility().summary().status())
				.isEqualTo(WorkoutFeasibilityStatus.FULLY_FEASIBLE);
		assertThat(application.finalWorkoutFeasibility().summary().feasibilityPercentage())
				.isEqualByComparingTo(new BigDecimal("100.00"));

		List<WorkoutExerciseSubstitutionResult> history = listWorkoutExerciseSubstitutionHistoryUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				occurrence.executions().get(0).id());
		history = new java.util.ArrayList<>(history);
		history.addAll(listWorkoutExerciseSubstitutionHistoryUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				occurrence.executions().get(1).id()));
		assertThat(history).hasSize(2);
		assertThat(history).allMatch(entry -> entry.workoutAdaptationProposalId() != null);
		assertThat(history).allMatch(entry -> entry.workoutAdaptationProposalItemId() != null);
		assertThat(history).allMatch(entry -> entry.adaptationDecisionSnapshot() != null);

		WorkoutOccurrenceDetailResult nextOccurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), LocalDate.of(2026, 6, 15), null, null);
		assertThat(nextOccurrence.executions().get(0).prescribedExerciseDefinitionId())
				.isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(nextOccurrence.executions().get(1).prescribedExerciseDefinitionId())
				.isEqualTo(SystemExerciseDefinitions.BENCH_PRESS);

		WorkoutOccurrenceDetailResult lockedOccurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), LocalDate.of(2026, 6, 22), null, null);
		WorkoutOccurrenceId lockedOccurrenceId = lockedOccurrence.occurrence().id();
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), lockedOccurrenceId, homeGym.id());
		WorkoutAdaptationProposalResult lockedProposal = generateWorkoutAdaptationProposalUseCase.execute(
				accountId, plan.id(), day.id(), lockedOccurrenceId, 3, false, 30);
		var firstSet = listWorkoutExerciseSetsUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				lockedOccurrenceId,
				lockedOccurrence.executions().get(0).id()).getFirst();
		startWorkoutExerciseSetUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				lockedOccurrenceId,
				lockedOccurrence.executions().get(0).id(),
				firstSet.id());
		WorkoutAdaptationProposalResult readyLockedProposal = updateWorkoutAdaptationProposalItemUseCase.execute(
				accountId,
				lockedProposal.id(),
				lockedProposal.items().get(0).id(),
				WorkoutAdaptationDecision.ACCEPTED,
				null,
				null,
				null);
		assertThatThrownBy(() -> applyWorkoutAdaptationProposalUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				lockedOccurrenceId,
				readyLockedProposal.id(),
				readyLockedProposal.version()))
				.isInstanceOf(WorkoutAdaptationProposalLockedException.class);

		WorkoutOccurrenceDetailResult rejectionOccurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), LocalDate.of(2026, 6, 29), null, null);
		WorkoutOccurrenceId rejectionOccurrenceId = rejectionOccurrence.occurrence().id();
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), rejectionOccurrenceId, homeGym.id());
		WorkoutAdaptationProposalResult rejectionProposal = generateWorkoutAdaptationProposalUseCase.execute(
				accountId, plan.id(), day.id(), rejectionOccurrenceId, 3, false, 30);
		updateWorkoutAdaptationProposalItemUseCase.execute(
				accountId,
				rejectionProposal.id(),
				rejectionProposal.items().get(0).id(),
				WorkoutAdaptationDecision.ACCEPTED,
				null,
				null,
				null);
		WorkoutAdaptationProposalResult rejectedReview = updateWorkoutAdaptationProposalItemUseCase.execute(
				accountId,
				rejectionProposal.id(),
				rejectionProposal.items().get(1).id(),
				WorkoutAdaptationDecision.REJECTED,
				null,
				null,
				"Skip bench");
		WorkoutAdaptationApplicationResult partialApplication = applyWorkoutAdaptationProposalUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				rejectionOccurrenceId,
				rejectedReview.id(),
				rejectedReview.version());
		assertThat(partialApplication.substitutionsApplied()).isEqualTo(1);
		assertThat(partialApplication.explicitlyExcludedExecutions()).isEqualTo(1);
		assertThat(partialApplication.finalWorkoutFeasibility().summary().status())
				.isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);
	}

	private static ExerciseDefinitionMetadata floorPress() {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STRENGTH,
				ExerciseMetricMode.WEIGHT_AND_REPETITIONS,
				MovementPattern.HORIZONTAL_PUSH,
				List.of(),
				List.of(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
				List.of(MuscleGroup.SHOULDERS),
				List.of(EquipmentType.DUMBBELL),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.OPEN_CHAIN,
				ImpactLevel.NO_IMPACT,
				ExerciseDifficulty.INTERMEDIATE);
	}

	private void prescribe(
			AccountId accountId,
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			ExerciseDefinitionId definitionId,
			String name,
			ExerciseType type) {
		createWorkoutExerciseUseCase.execute(
				accountId,
				planId,
				dayId,
				definitionId,
				name,
				ExerciseCategory.STRENGTH,
				type,
				3,
				8,
				12,
				new BigDecimal("40"),
				WeightUnit.KILOGRAM,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Morgan",
				"Lee",
				LocalDate.of(1995, 8, 2),
				Sex.MALE,
				Height.ofCentimeters(180),
				Weight.ofKilograms(82),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		return accountId;
	}

}
