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

import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
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
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingClientContractVersion;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalOrigin;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

import jakarta.persistence.EntityManagerFactory;

@SpringBootTest
@Import({
		TestcontainersConfiguration.class,
		TrainingClientFacadeAcceptanceIntegrationTests.MutableClockConfig.class
})
class TrainingClientFacadeAcceptanceIntegrationTests {

	private static final LocalDate JULY_31 = LocalDate.of(2026, 7, 31);
	private static final int BASELINE_WINDOW = 7;

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private AthleteContextPort athleteContextPort;

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
	private GetDailyRecoveryCheckInByDateUseCase getDailyRecoveryCheckInByDateUseCase;

	@Autowired
	private GenerateDailyAthleteStateSnapshotUseCase generateDailyAthleteStateSnapshotUseCase;

	@Autowired
	private GenerateDailyReadinessAssessmentUseCase generateDailyReadinessAssessmentUseCase;

	@Autowired
	private GenerateDailyTrainingRecommendationUseCase generateDailyTrainingRecommendationUseCase;

	@Autowired
	private GenerateRecommendedWorkoutAdaptationProposalUseCase generateRecommendedAdaptationUseCase;

	@Autowired
	private GetTrainingTodayDashboardUseCase getTrainingTodayDashboardUseCase;

	@Autowired
	private GetTrainingOverviewUseCase getTrainingOverviewUseCase;

	@Autowired
	private GetRecoveryOverviewUseCase getRecoveryOverviewUseCase;

	@Autowired
	private GetWorkoutLaunchContextUseCase getWorkoutLaunchContextUseCase;

	@Autowired
	private GetTrainingClientBootstrapUseCase getTrainingClientBootstrapUseCase;

	@Autowired
	private DailyAthleteStateSnapshotRepository snapshotRepository;

	@Autowired
	private DailyReadinessAssessmentRepository readinessRepository;

	@Autowired
	private DailyTrainingRecommendationRepository recommendationRepository;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void criticalTodayDashboardAndLaunchContextComposeExistingFactsWithoutMutation() {
		PopulatedDay day = populatedModifySessionDay();

		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		sessionFactory.getStatistics().setStatisticsEnabled(true);
		sessionFactory.getStatistics().clear();

		TrainingTodayDashboardResult dashboard = getTrainingTodayDashboardUseCase.execute(
				day.accountId(), JULY_31);
		// Two extra owned-child reads vs the prior 15-query cap: stored
		// limiting dimensions and recommendation adjustment types.
		assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(17);

		assertThat(dashboard.date()).isEqualTo(JULY_31);
		assertThat(dashboard.recovery().checkInPresent()).isTrue();
		assertThat(dashboard.recovery().recoveryCheckInId()).isEqualTo(day.checkInId());
		assertThat(dashboard.athleteState().snapshotPresent()).isTrue();
		assertThat(dashboard.athleteState().dailyAthleteStateSnapshotId()).isEqualTo(day.snapshotId());
		assertThat(dashboard.athleteState().snapshotVersion()).isEqualTo(day.snapshotVersion());
		assertThat(dashboard.readiness().readinessPresent()).isTrue();
		assertThat(dashboard.readiness().readinessAssessmentId()).isEqualTo(day.readinessAssessmentId());
		assertThat(dashboard.readiness().readinessScore()).isEqualByComparingTo(day.readinessScore());
		assertThat(dashboard.readiness().readinessBand()).isEqualTo(ReadinessBand.LOW);
		assertThat(dashboard.readiness().readinessScore()).isLessThan(new BigDecimal("50"));
		assertThat(dashboard.readiness().limitingDimensions()).contains(ReadinessDimensionType.MUSCLE_SORENESS);
		assertThat(dashboard.recommendation().recommendationPresent()).isTrue();
		assertThat(dashboard.recommendation().recommendationId()).isEqualTo(day.recommendationId());
		assertThat(dashboard.recommendation().overallAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
		assertThat(dashboard.recommendation().adjustmentTypes()).contains(
				TrainingAdjustmentType.REDUCE_INTENSITY,
				TrainingAdjustmentType.REDUCE_TOTAL_VOLUME);
		assertThat(dashboard.training().primaryOccurrence()).isNotNull();
		assertThat(dashboard.training().primaryOccurrence().occurrenceId()).isEqualTo(day.occurrenceId());
		assertThat(dashboard.training().primaryOccurrence().plannedEnvironmentName()).contains("Home Gym");
		assertThat(dashboard.training().primaryOccurrence().feasibilityStatus())
				.isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);
		assertThat(dashboard.adaptation().activeProposalPresent()).isTrue();
		assertThat(dashboard.adaptation().adaptationProposalId()).isEqualTo(day.proposalId());
		assertThat(dashboard.adaptation().origin()).isEqualTo(WorkoutAdaptationProposalOrigin.TRAINING_RECOMMENDATION);
		assertThat(dashboard.actions().canGenerateAdaptationProposal().allowed()).isFalse();
		assertThat(dashboard.actions().canStartWorkout().allowed()).isTrue();

		AthleteId athleteId = athleteId(day.accountId());
		int snapshotCountBefore = snapshotRepository.findHistory(
				athleteId, JULY_31, JULY_31, true, 0, 10).size();
		int readinessCountBefore = readinessRepository.findHistory(
				athleteId, JULY_31, JULY_31, true, null, 0, 10).size();
		int recommendationCountBefore = recommendationRepository.findHistory(
				athleteId, JULY_31, JULY_31, true, null, null, 0, 10).size();

		sessionFactory.getStatistics().clear();
		WorkoutLaunchContextResult launch = getWorkoutLaunchContextUseCase.execute(
				day.accountId(),
				day.planId(),
				day.dayId(),
				WorkoutOccurrenceId.of(day.occurrenceId()));
		assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(15);

		assertThat(launch.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.SCHEDULED);
		assertThat(launch.exercises()).isNotEmpty();
		assertThat(launch.feasibility().feasibilityPresent()).isTrue();
		assertThat(launch.feasibility().status()).isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);
		assertThat(launch.recommendationContext().recommendationPresent()).isTrue();
		assertThat(launch.recommendationContext().recommendationId()).isEqualTo(day.recommendationId());
		assertThat(launch.adaptation().activeProposalPresent()).isTrue();
		assertThat(launch.adaptation().adaptationProposalId()).isEqualTo(day.proposalId());
		assertThat(launch.actions().canStart().allowed()).isTrue();
		assertThat(launch.actions().canGenerateAdaptation().allowed()).isFalse();

		assertThat(snapshotRepository.findHistory(athleteId, JULY_31, JULY_31, true, 0, 10))
				.hasSize(snapshotCountBefore);
		assertThat(readinessRepository.findHistory(athleteId, JULY_31, JULY_31, true, null, 0, 10))
				.hasSize(readinessCountBefore);
		assertThat(recommendationRepository.findHistory(athleteId, JULY_31, JULY_31, true, null, null, 0, 10))
				.hasSize(recommendationCountBefore);
	}

	@Test
	void todayDashboardDoesNotAutoGenerateMissingSnapshotReadinessOrRecommendation() {
		AccountId accountId = athlete();
		seedPriorCheckIns(accountId);
		scheduleBench(accountId, JULY_31, true);
		createDailyRecoveryCheckInUseCase.execute(
				accountId, JULY_31, 360, 3, 5, 5, 2, 3, 3,
				List.of(new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 2, null)), null);

		AthleteId athleteId = athleteId(accountId);
		assertThat(snapshotRepository.findCurrentByAthleteIdAndStateDate(athleteId, JULY_31)).isEmpty();

		TrainingTodayDashboardResult dashboard = getTrainingTodayDashboardUseCase.execute(accountId, JULY_31);
		assertThat(dashboard.recovery().checkInPresent()).isTrue();
		assertThat(dashboard.athleteState().snapshotPresent()).isFalse();
		assertThat(dashboard.readiness().readinessPresent()).isFalse();
		assertThat(dashboard.readiness().limitingDimensions()).isEmpty();
		assertThat(dashboard.recommendation().recommendationPresent()).isFalse();
		assertThat(dashboard.recommendation().adjustmentTypes()).isEmpty();
		assertThat(dashboard.actions().canGenerateAthleteStateSnapshot().allowed()).isTrue();
		assertThat(dashboard.actions().canGenerateReadinessAssessment().allowed()).isFalse();
		assertThat(dashboard.actions().canGenerateReadinessAssessment().reasonCode())
				.isEqualTo("DAILY_ATHLETE_STATE_SNAPSHOT_REQUIRED");

		assertThat(snapshotRepository.findCurrentByAthleteIdAndStateDate(athleteId, JULY_31)).isEmpty();
		assertThat(readinessRepository.findHistory(athleteId, JULY_31, JULY_31, true, null, 0, 10)).isEmpty();
		assertThat(recommendationRepository.findHistory(athleteId, JULY_31, JULY_31, true, null, null, 0, 10))
				.isEmpty();
	}

	@Test
	void emptyStatesAndPrimarySelectionRemainStable() {
		AccountId accountId = athlete();
		TrainingTodayDashboardResult empty = getTrainingTodayDashboardUseCase.execute(accountId, JULY_31);
		assertThat(empty.recovery().checkInPresent()).isFalse();
		assertThat(empty.athleteState().snapshotPresent()).isFalse();
		assertThat(empty.readiness().readinessPresent()).isFalse();
		assertThat(empty.readiness().limitingDimensions()).isEmpty();
		assertThat(empty.recommendation().recommendationPresent()).isFalse();
		assertThat(empty.recommendation().adjustmentTypes()).isEmpty();
		assertThat(empty.training().primaryOccurrence()).isNull();
		assertThat(empty.adaptation().activeProposalPresent()).isFalse();

		seedPriorCheckIns(accountId);
		ScheduledWorkout first = scheduleBench(accountId, JULY_31, true);
		ScheduledWorkout second = scheduleBench(accountId, JULY_31, true);
		createDailyRecoveryCheckInUseCase.execute(
				accountId, JULY_31, 360, 3, 5, 5, 2, 3, 3, null, null);

		TrainingTodayDashboardResult withScheduled = getTrainingTodayDashboardUseCase.execute(accountId, JULY_31);
		assertThat(withScheduled.training().occurrences()).hasSizeGreaterThanOrEqualTo(2);
		assertThat(withScheduled.training().primaryOccurrence().status())
				.isEqualTo(WorkoutOccurrenceStatus.SCHEDULED);
		assertThat(withScheduled.training().primaryOccurrence().occurrenceId())
				.isIn(first.occurrenceId(), second.occurrenceId());
	}

	@Test
	void overviewRecoveryBootstrapAndInvalidInputs() {
		PopulatedDay day = populatedModifySessionDay();

		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		sessionFactory.getStatistics().setStatisticsEnabled(true);
		sessionFactory.getStatistics().clear();
		TrainingOverviewResult overview = getTrainingOverviewUseCase.execute(day.accountId(), JULY_31);
		assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(15);
		assertThat(overview.activePlans()).isNotEmpty();
		assertThat(overview.upcomingOccurrences()).isNotEmpty();
		assertThat(overview.activeEnvironments()).isNotEmpty();
		assertThat(overview.outstandingAdaptationProposals()).isNotEmpty();

		sessionFactory.getStatistics().clear();
		RecoveryOverviewResult recovery = getRecoveryOverviewUseCase.execute(day.accountId(), JULY_31, 7);
		assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(12);
		assertThat(recovery.checkInPresent()).isTrue();
		assertThat(recovery.readinessPresent()).isTrue();
		assertThat(recovery.recommendationPresent()).isTrue();
		assertThat(recovery.trends()).isNotEmpty();

		TrainingClientBootstrapResult bootstrap = getTrainingClientBootstrapUseCase.execute(day.accountId());
		assertThat(bootstrap.clientContractVersion()).isEqualTo(TrainingClientContractVersion.V1);
		assertThat(bootstrap.features().readinessEnabled()).isTrue();
		assertThat(bootstrap.features().recommendationsEnabled()).isTrue();
		assertThat(bootstrap.features().adaptationEnabled()).isTrue();
		assertThat(bootstrap.limits().baselineWindows()).containsExactly(7, 14, 28);
		assertThat(bootstrap.limits().readinessAlgorithmVersion()).isEqualTo(ReadinessAlgorithmVersion.READINESS_V1);
		assertThat(bootstrap.limits().recommendationAlgorithmVersion())
				.isEqualTo(TrainingRecommendationAlgorithmVersion.TRAINING_RECOMMENDATION_V1);

		assertThatThrownBy(() -> getTrainingTodayDashboardUseCase.execute(
				day.accountId(), LocalDate.of(2099, 1, 1)))
				.isInstanceOf(InvalidTrainingClientDateException.class);
		assertThatThrownBy(() -> getRecoveryOverviewUseCase.execute(day.accountId(), JULY_31, 9))
				.isInstanceOf(InvalidTrainingClientTrendDaysException.class);
	}

	private PopulatedDay populatedModifySessionDay() {
		AccountId accountId = athlete();
		seedPriorCheckIns(accountId);
		ScheduledWorkout scheduled = scheduleBench(accountId, JULY_31, true);
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
				scheduled.occurrenceId(),
				3,
				true,
				30);
		DailyRecoveryCheckInResult checkIn = getDailyRecoveryCheckInByDateUseCase.execute(accountId, JULY_31);
		return new PopulatedDay(
				accountId,
				scheduled.planId(),
				scheduled.dayId(),
				scheduled.occurrenceId(),
				checkIn.id().value(),
				snapshot.snapshotId(),
				snapshot.snapshotVersion(),
				readiness.assessmentId(),
				readiness.readinessScore(),
				recommendation.recommendationId(),
				proposal.id().value());
	}

	private ScheduledWorkout scheduleBench(AccountId accountId, LocalDate date, boolean withEnvironment) {
		TrainingEnvironmentResult environment = null;
		if (withEnvironment) {
			environment = createTrainingEnvironmentUseCase.execute(
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
			ExerciseDefinitionId alternative = createAthleteExerciseDefinitionUseCase.execute(
					accountId,
					"DB Bench " + UUID.randomUUID(),
					ExerciseDefinitionMetadata.of(
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
							ImpactLevel.LOW_IMPACT,
							ExerciseDifficulty.INTERMEDIATE)).id();
			createExerciseSubstitutionRelationshipUseCase.execute(
					accountId,
					SystemExerciseDefinitions.BENCH_PRESS,
					alternative,
					ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
					ExerciseSubstitutionCompatibility.HIGH,
					"Home option");
		}
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Client Facade " + UUID.randomUUID(), null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null,
				environment == null ? null : environment.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Upper", null, 1, DayOfWeek.FRIDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BENCH_PRESS, "Bench Press",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("80"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, 0);
		// Plank is environment-feasible with OPEN_SPACE; bench is not — PARTIALLY_FEASIBLE overall.
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.PLANK, "Plank",
				ExerciseCategory.STRENGTH, ExerciseType.BODYWEIGHT,
				3, null, null, null, null,
				60, null, null, null, null, null, null, 1);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), date, null, null);
		if (environment != null) {
			setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
					accountId, plan.id(), day.id(), occurrence.occurrence().id(), environment.id());
		}
		return new ScheduledWorkout(plan.id(), day.id(), occurrence.occurrence().id().value());
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

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Jordan", "Lee", LocalDate.of(1993, 4, 11), Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(82),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		return accountId;
	}

	private AthleteId athleteId(AccountId accountId) {
		return AthleteId.of(athleteContextPort.requireAthlete(accountId.value()).athleteId());
	}

	private record ScheduledWorkout(
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			UUID occurrenceId) {
	}

	private record PopulatedDay(
			AccountId accountId,
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			UUID occurrenceId,
			UUID checkInId,
			UUID snapshotId,
			int snapshotVersion,
			UUID readinessAssessmentId,
			BigDecimal readinessScore,
			UUID recommendationId,
			UUID proposalId) {
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
