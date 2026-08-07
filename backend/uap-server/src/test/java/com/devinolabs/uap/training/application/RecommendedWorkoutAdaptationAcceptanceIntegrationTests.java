package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionMetadata;
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
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingAdjustmentApplicability;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutAdaptationDecision;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalOrigin;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@Import({
		TestcontainersConfiguration.class,
		RecommendedWorkoutAdaptationAcceptanceIntegrationTests.MutableClockConfig.class
})
class RecommendedWorkoutAdaptationAcceptanceIntegrationTests {

	private static final LocalDate JULY_31 = LocalDate.of(2026, 7, 31);
	private static final int BASELINE_WINDOW = 7;

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
	private CreateDailyRecoveryCheckInUseCase createDailyRecoveryCheckInUseCase;

	@Autowired
	private GenerateDailyAthleteStateSnapshotUseCase generateDailyAthleteStateSnapshotUseCase;

	@Autowired
	private GenerateDailyReadinessAssessmentUseCase generateDailyReadinessAssessmentUseCase;

	@Autowired
	private GenerateDailyTrainingRecommendationUseCase generateDailyTrainingRecommendationUseCase;

	@Autowired
	private GenerateRecommendedWorkoutAdaptationProposalUseCase generateRecommendedAdaptationUseCase;

	@Autowired
	private GenerateWorkoutAdaptationProposalUseCase generateManualAdaptationUseCase;

	@Autowired
	private GetWorkoutAdaptationProposalUseCase getWorkoutAdaptationProposalUseCase;

	@Autowired
	private UpdateWorkoutAdaptationProposalItemUseCase updateWorkoutAdaptationProposalItemUseCase;

	@Autowired
	private ApplyWorkoutAdaptationProposalUseCase applyWorkoutAdaptationProposalUseCase;

	@Autowired
	private ListWorkoutExerciseSubstitutionHistoryUseCase listWorkoutExerciseSubstitutionHistoryUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private StartWorkoutExerciseSetUseCase startWorkoutExerciseSetUseCase;

	@Autowired
	private GetDailyTrainingRecommendationUseCase getDailyTrainingRecommendationUseCase;

	@Test
	void criticalRecommendationToAdaptationFlowPreservesProvenanceAndLowerImpactRanking() {
		AccountId accountId = athlete();
		seedPriorCheckIns(accountId);

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

		ExerciseDefinitionId moderateImpactId = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "DB Bench Moderate Impact", pushMetadata(ImpactLevel.MODERATE_IMPACT)).id();
		ExerciseDefinitionId lowImpactId = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "DB Bench Low Impact", pushMetadata(ImpactLevel.LOW_IMPACT)).id();
		createExerciseSubstitutionRelationshipUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BENCH_PRESS,
				moderateImpactId,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				ExerciseSubstitutionCompatibility.HIGH,
				"Moderate impact home option");
		createExerciseSubstitutionRelationshipUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BENCH_PRESS,
				lowImpactId,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				ExerciseSubstitutionCompatibility.HIGH,
				"Lower impact home option");

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId,
				TrainingPlanType.STRENGTH,
				null,
				"Recommendation Adaptation Block",
				null,
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 12, 31),
				null,
				null,
				homeGym.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Upper Body", null, 1, DayOfWeek.FRIDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BENCH_PRESS, "Bench Press",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("80"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, 0);

		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), JULY_31, null, null);
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, homeGym.id());

		createDailyRecoveryCheckInUseCase.execute(
				accountId,
				JULY_31,
				360,
				3,
				5,
				5,
				2,
				3,
				3,
				List.of(new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 2, "Mild tightness")),
				null);

		DailyAthleteStateSnapshotResult snapshot = generateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		DailyReadinessAssessmentResult readiness = generateDailyReadinessAssessmentUseCase.execute(
				accountId, snapshot.snapshotId());
		assertThat(readiness.limitingDimensions()).contains(ReadinessDimensionType.MUSCLE_SORENESS);
		DailyTrainingRecommendationResult recommendation = generateDailyTrainingRecommendationUseCase.execute(
				accountId, readiness.assessmentId());

		assertThat(recommendation.overallAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
		assertThat(recommendation.adjustments()).extracting(a -> a.type())
				.contains(
						TrainingAdjustmentType.REDUCE_INTENSITY,
						TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
						TrainingAdjustmentType.PREFER_LOWER_IMPACT_VARIATIONS);
		assertThat(recommendation.scheduledOccurrences()).extracting(o -> o.occurrenceId())
				.containsExactly(occurrenceId.value());

		WorkoutAdaptationProposalResult proposal = generateRecommendedAdaptationUseCase.execute(
				accountId,
				recommendation.recommendationId(),
				occurrenceId.value(),
				3,
				true,
				30);

		assertThat(proposal.origin()).isEqualTo(WorkoutAdaptationProposalOrigin.TRAINING_RECOMMENDATION);
		assertThat(proposal.recommendationProvenance()).isNotNull();
		assertThat(proposal.recommendationProvenance().recommendationId())
				.isEqualTo(recommendation.recommendationId());
		assertThat(proposal.recommendationProvenance().readinessAssessmentId())
				.isEqualTo(readiness.assessmentId());
		assertThat(proposal.recommendationProvenance().stateSnapshotId())
				.isEqualTo(snapshot.snapshotId());
		assertThat(proposal.recommendationProvenance().overallAction())
				.isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
		assertThat(proposal.recommendationProvenance().readinessBand()).isEqualTo(readiness.readinessBand());
		assertThat(proposal.recommendationAdjustments()).isNotEmpty();
		assertThat(proposal.recommendationAdjustments())
				.anySatisfy(adjustment -> {
					assertThat(adjustment.type()).isEqualTo(TrainingAdjustmentType.REDUCE_INTENSITY);
					assertThat(adjustment.applicability()).isEqualTo(TrainingAdjustmentApplicability.CONTEXT_ONLY);
				});
		assertThat(proposal.recommendationAdjustments())
				.anySatisfy(adjustment -> {
					assertThat(adjustment.type()).isEqualTo(TrainingAdjustmentType.PREFER_LOWER_IMPACT_VARIATIONS);
					assertThat(adjustment.applicability())
							.isEqualTo(TrainingAdjustmentApplicability.CONCRETELY_APPLICABLE);
				});
		assertThat(proposal.proposedSubstitutions()).isEqualTo(1);
		assertThat(proposal.items()).hasSize(1);

		WorkoutAdaptationProposalItemResult item = proposal.items().getFirst();
		assertThat(item.generatedTargetExerciseDefinitionId()).isEqualTo(lowImpactId);
		assertThat(item.alternatives()).isNotEmpty();
		assertThat(item.alternatives().getFirst().targetExerciseDefinitionId()).isEqualTo(lowImpactId);
		assertThat(item.alternatives()).extracting(a -> a.targetExerciseDefinitionId())
				.containsSubsequence(lowImpactId, moderateImpactId);

		WorkoutAdaptationProposalResult reviewed = updateWorkoutAdaptationProposalItemUseCase.execute(
				accountId,
				proposal.id(),
				item.id(),
				WorkoutAdaptationDecision.ACCEPTED,
				null,
				null,
				null);
		WorkoutAdaptationApplicationResult applied = applyWorkoutAdaptationProposalUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				reviewed.id(),
				reviewed.version());
		assertThat(applied.proposalStatus()).isEqualTo(WorkoutAdaptationProposalStatus.APPLIED);
		assertThat(applied.substitutionsApplied()).isEqualTo(1);

		List<WorkoutExerciseSubstitutionResult> history = listWorkoutExerciseSubstitutionHistoryUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				occurrenceId,
				occurrence.executions().getFirst().id());
		assertThat(history).hasSize(1);
		assertThat(history.getFirst().workoutAdaptationProposalId()).isEqualTo(proposal.id());

		WorkoutAdaptationProposalResult historicalProposal = getWorkoutAdaptationProposalUseCase.execute(
				accountId, proposal.id());
		assertThat(historicalProposal.recommendationProvenance().recommendationId())
				.isEqualTo(recommendation.recommendationId());
		assertThat(historicalProposal.recommendationAdjustments())
				.extracting(a -> a.type())
				.contains(
						TrainingAdjustmentType.REDUCE_INTENSITY,
						TrainingAdjustmentType.REDUCE_TOTAL_VOLUME,
						TrainingAdjustmentType.PREFER_LOWER_IMPACT_VARIATIONS);

		createDailyRecoveryCheckInUseCase.execute(
				accountId,
				LocalDate.of(2026, 8, 1),
				480,
				4,
				2,
				2,
				2,
				4,
				4,
				null,
				null);
		DailyAthleteStateSnapshotResult nextSnapshot = generateDailyAthleteStateSnapshotUseCase.execute(
				accountId, LocalDate.of(2026, 8, 1), BASELINE_WINDOW);
		DailyReadinessAssessmentResult nextReadiness = generateDailyReadinessAssessmentUseCase.execute(
				accountId, nextSnapshot.snapshotId());
		DailyTrainingRecommendationResult nextRecommendation = generateDailyTrainingRecommendationUseCase.execute(
				accountId, nextReadiness.assessmentId());
		assertThat(nextRecommendation.recommendationId()).isNotEqualTo(recommendation.recommendationId());

		WorkoutAdaptationProposalResult stillOld = getWorkoutAdaptationProposalUseCase.execute(
				accountId, proposal.id());
		assertThat(stillOld.recommendationProvenance().recommendationId())
				.isEqualTo(recommendation.recommendationId());
		assertThat(stillOld.recommendationProvenance().stateSnapshotId())
				.isEqualTo(snapshot.snapshotId());
	}

	@Test
	void lockedOccurrenceRejectsRecommendedGenerationWithoutMutatingRecommendation() {
		Fixture fixture = fixtureWithModifySession(athlete());

		var firstSet = listWorkoutExerciseSetsUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				fixture.occurrenceId(),
				fixture.executionId()).getFirst();
		startWorkoutExerciseSetUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				fixture.occurrenceId(),
				fixture.executionId(),
				firstSet.id());

		assertThatThrownBy(() -> generateRecommendedAdaptationUseCase.execute(
				fixture.accountId(),
				fixture.recommendationId(),
				fixture.occurrenceId().value(),
				3,
				true,
				30))
				.isInstanceOf(RecommendedAdaptationOccurrenceLockedException.class);

		DailyTrainingRecommendationResult recommendation = getDailyTrainingRecommendationUseCase.execute(
				fixture.accountId(), fixture.recommendationId());
		assertThat(recommendation.overallAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
	}

	@Test
	void rejectsForeignOccurrenceAndNonModifyActionsAndActiveProposalConflicts() {
		Fixture fixture = fixtureWithModifySession(athlete());

		WorkoutOccurrenceDetailResult otherOccurrence = createWorkoutOccurrenceUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				LocalDate.of(2026, 8, 7),
				null,
				null);
		assertThatThrownBy(() -> generateRecommendedAdaptationUseCase.execute(
				fixture.accountId(),
				fixture.recommendationId(),
				otherOccurrence.occurrence().id().value(),
				3,
				false,
				30))
				.isInstanceOf(TrainingRecommendationOccurrenceMismatchException.class);

		WorkoutAdaptationProposalResult manual = generateManualAdaptationUseCase.execute(
				fixture.accountId(),
				fixture.planId(),
				fixture.dayId(),
				fixture.occurrenceId(),
				3,
				false,
				30);
		assertThat(manual.origin()).isEqualTo(WorkoutAdaptationProposalOrigin.MANUAL);
		assertThat(manual.recommendationProvenance()).isNull();

		assertThatThrownBy(() -> generateRecommendedAdaptationUseCase.execute(
				fixture.accountId(),
				fixture.recommendationId(),
				fixture.occurrenceId().value(),
				3,
				false,
				30))
				.isInstanceOf(ActiveWorkoutAdaptationProposalExistsException.class);

		AccountId highReadinessAthlete = athlete();
		seedPriorCheckIns(highReadinessAthlete);
		scheduleBenchOnly(highReadinessAthlete, JULY_31, null);
		createDailyRecoveryCheckInUseCase.execute(
				highReadinessAthlete,
				JULY_31,
				480,
				5,
				1,
				1,
				1,
				5,
				5,
				null,
				null);
		DailyAthleteStateSnapshotResult snapshot = generateDailyAthleteStateSnapshotUseCase.execute(
				highReadinessAthlete, JULY_31, BASELINE_WINDOW);
		DailyReadinessAssessmentResult readiness = generateDailyReadinessAssessmentUseCase.execute(
				highReadinessAthlete, snapshot.snapshotId());
		DailyTrainingRecommendationResult proceed = generateDailyTrainingRecommendationUseCase.execute(
				highReadinessAthlete, readiness.assessmentId());
		assertThat(proceed.overallAction()).isIn(
				TrainingRecommendationAction.PROCEED_AS_PLANNED,
				TrainingRecommendationAction.MODIFY_SESSION);
		if (proceed.overallAction() != TrainingRecommendationAction.MODIFY_SESSION) {
			assertThatThrownBy(() -> generateRecommendedAdaptationUseCase.execute(
					highReadinessAthlete,
					proceed.recommendationId(),
					proceed.scheduledOccurrences().getFirst().occurrenceId(),
					3,
					false,
					30))
					.isInstanceOf(TrainingRecommendationNotAdaptationEligibleException.class);
		}
	}

	@Test
	void feasibleExecutionIsNotProactivelySubstitutedFromLowerImpactPreferenceAlone() {
		AccountId accountId = athlete();
		seedPriorCheckIns(accountId);
		TrainingEnvironmentResult homeGym = createTrainingEnvironmentUseCase.execute(
				accountId,
				"Bodyweight Space",
				TrainingEnvironmentType.HOME_GYM,
				List.of(EquipmentType.BODYWEIGHT, EquipmentType.OPEN_SPACE),
				null,
				null,
				true);
		ExerciseDefinitionId lowerImpactPlank = createAthleteExerciseDefinitionUseCase.execute(
				accountId,
				"Kneeling Plank",
				ExerciseDefinitionMetadata.of(
						ExerciseDefinitionCategory.STRENGTH,
						ExerciseMetricMode.DURATION,
						MovementPattern.ISOMETRIC,
						List.of(),
						List.of(MuscleGroup.ABDOMINALS),
						List.of(),
						List.of(EquipmentType.BODYWEIGHT),
						List.of(),
						ExerciseLaterality.BILATERAL,
						KineticChainType.CLOSED_CHAIN,
						ImpactLevel.NO_IMPACT,
						ExerciseDifficulty.BEGINNER)).id();
		createExerciseSubstitutionRelationshipUseCase.execute(
				accountId,
				SystemExerciseDefinitions.PLANK,
				lowerImpactPlank,
				ExerciseSubstitutionRelationshipType.LOWER_IMPACT_ALTERNATIVE,
				ExerciseSubstitutionCompatibility.HIGH,
				"Optional lower impact");

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Feasible Only", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null, homeGym.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Core", null, 1, DayOfWeek.FRIDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.PLANK, "Plank",
				ExerciseCategory.STRENGTH, ExerciseType.BODYWEIGHT,
				3, null, null, null, null,
				60, null, null, null, null, null, null, null);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), JULY_31, null, null);
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), occurrence.occurrence().id(), homeGym.id());

		createDailyRecoveryCheckInUseCase.execute(
				accountId, JULY_31, 360, 3, 5, 5, 2, 3, 3,
				List.of(new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 2, null)), null);
		DailyAthleteStateSnapshotResult snapshot = generateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		DailyReadinessAssessmentResult readiness = generateDailyReadinessAssessmentUseCase.execute(
				accountId, snapshot.snapshotId());
		DailyTrainingRecommendationResult recommendation = generateDailyTrainingRecommendationUseCase.execute(
				accountId, readiness.assessmentId());
		assertThat(recommendation.overallAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);

		WorkoutAdaptationProposalResult proposal = generateRecommendedAdaptationUseCase.execute(
				accountId,
				recommendation.recommendationId(),
				occurrence.occurrence().id().value(),
				3,
				true,
				30);
		assertThat(proposal.proposedSubstitutions()).isZero();
		assertThat(proposal.items()).allMatch(WorkoutAdaptationProposalItemResult::currentFeasible);
	}

	private Fixture fixtureWithModifySession(AccountId accountId) {
		seedPriorCheckIns(accountId);
		ScheduledWorkout scheduled = scheduleBenchOnly(accountId, JULY_31, homeGym(accountId));
		createDailyRecoveryCheckInUseCase.execute(
				accountId,
				JULY_31,
				360,
				3,
				5,
				5,
				2,
				3,
				3,
				List.of(new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 2, null)),
				null);
		DailyAthleteStateSnapshotResult snapshot = generateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		DailyReadinessAssessmentResult readiness = generateDailyReadinessAssessmentUseCase.execute(
				accountId, snapshot.snapshotId());
		DailyTrainingRecommendationResult recommendation = generateDailyTrainingRecommendationUseCase.execute(
				accountId, readiness.assessmentId());
		assertThat(recommendation.overallAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
		return new Fixture(
				accountId,
				scheduled.planId(),
				scheduled.dayId(),
				scheduled.occurrenceId(),
				scheduled.executionId(),
				recommendation.recommendationId());
	}

	private TrainingEnvironmentResult homeGym(AccountId accountId) {
		return createTrainingEnvironmentUseCase.execute(
				accountId,
				"Home Gym " + UUID.randomUUID(),
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
	}

	private ScheduledWorkout scheduleBenchOnly(
			AccountId accountId,
			LocalDate scheduledDate,
			TrainingEnvironmentResult environment) {
		ExerciseDefinitionId dumbbellBench = createAthleteExerciseDefinitionUseCase.execute(
				accountId,
				"DB Bench " + UUID.randomUUID(),
				pushMetadata(ImpactLevel.LOW_IMPACT)).id();
		createExerciseSubstitutionRelationshipUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BENCH_PRESS,
				dumbbellBench,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				ExerciseSubstitutionCompatibility.HIGH,
				"Home option");
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Block " + UUID.randomUUID(), null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null,
				environment == null ? null : environment.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Bench Day", null, 1, DayOfWeek.FRIDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BENCH_PRESS, "Bench Press",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("80"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, 0);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), scheduledDate, null, null);
		if (environment != null) {
			setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
					accountId, plan.id(), day.id(), occurrence.occurrence().id(), environment.id());
		}
		return new ScheduledWorkout(
				plan.id(),
				day.id(),
				occurrence.occurrence().id(),
				occurrence.executions().getFirst().id());
	}

	private void seedPriorCheckIns(AccountId accountId) {
		createCheckIn(accountId, LocalDate.of(2026, 7, 24), 420, 3, 3, 2, 2, 4, 4);
		createCheckIn(accountId, LocalDate.of(2026, 7, 25), 450, 4, 2, 2, 3, 4, 4);
		createCheckIn(accountId, LocalDate.of(2026, 7, 26), 390, 3, 4, 3, 3, 3, 3);
		createCheckIn(accountId, LocalDate.of(2026, 7, 27), 435, 4, 3, 3, 2, 4, 4);
		createCheckIn(accountId, LocalDate.of(2026, 7, 28), 420, 3, 3, 2, 3, 4, 3);
		createCheckIn(accountId, LocalDate.of(2026, 7, 29), 405, 3, 4, 3, 4, 3, 3);
		createCheckIn(accountId, LocalDate.of(2026, 7, 30), 450, 4, 3, 2, 2, 4, 4);
	}

	private void createCheckIn(
			AccountId accountId,
			LocalDate date,
			int sleepMinutes,
			int sleepQuality,
			int fatigue,
			int soreness,
			int stress,
			int mood,
			int motivation) {
		createDailyRecoveryCheckInUseCase.execute(
				accountId, date, sleepMinutes, sleepQuality, fatigue, soreness, stress, mood, motivation, null, null);
	}

	private static ExerciseDefinitionMetadata pushMetadata(ImpactLevel impactLevel) {
		return ExerciseDefinitionMetadata.of(
				ExerciseDefinitionCategory.STRENGTH,
				ExerciseMetricMode.WEIGHT_AND_REPETITIONS,
				MovementPattern.HORIZONTAL_PUSH,
				List.of(),
				List.of(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
				List.of(MuscleGroup.SHOULDERS),
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH),
				List.of(),
				ExerciseLaterality.BILATERAL,
				KineticChainType.OPEN_CHAIN,
				impactLevel,
				ExerciseDifficulty.INTERMEDIATE);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Riley", "Chen", LocalDate.of(1994, 3, 12), Sex.FEMALE,
				Height.ofCentimeters(168), Weight.ofKilograms(62),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		return accountId;
	}

	private record Fixture(
			AccountId accountId,
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId executionId,
			UUID recommendationId) {
	}

	private record ScheduledWorkout(
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId executionId) {
	}

	@TestConfiguration
	static class MutableClockConfig {

		@Bean
		@Primary
		Clock mutableClock() {
			return Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC);
		}

	}

}
