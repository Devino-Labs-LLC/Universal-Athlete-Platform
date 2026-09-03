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
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutAdaptationProposalOrigin;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutFeasibilityStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

import jakarta.persistence.EntityManagerFactory;

/**
 * Phase 7V release-candidate cross-feature suite.
 *
 * <p>Focused smoke of the client-critical path. Deferrals:
 * <ul>
 *   <li>RC01 schema/Flyway bootstrap — covered by {@code UapServerApplicationTests}; not duplicated.</li>
 *   <li>RC12 completion-metric atomicity — deferred to {@link CompletionMetricAtomicityIntegrationTests}
 *       (requires failing PR repository fixture).</li>
 *   <li>RC13/RC14 concurrency / optimistic locking — deferred to
 *       {@link WorkoutOccurrenceConsistencyIntegrationTests} and
 *       {@link CanonicalExerciseSubstitutionIntegrationTests}.</li>
 * </ul>
 */
@SpringBootTest
@Import({
		TestcontainersConfiguration.class,
		TrainingReleaseCandidateIntegrationTests.MutableClockConfig.class
})
class TrainingReleaseCandidateIntegrationTests {

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
	private AnalyzeWorkoutOccurrenceFeasibilityUseCase analyzeWorkoutOccurrenceFeasibilityUseCase;

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
	private GetTrainingTodayDashboardUseCase getTrainingTodayDashboardUseCase;

	@Autowired
	private GetTrainingOverviewUseCase getTrainingOverviewUseCase;

	@Autowired
	private GetRecoveryOverviewUseCase getRecoveryOverviewUseCase;

	@Autowired
	private GetWorkoutLaunchContextUseCase getWorkoutLaunchContextUseCase;

	@Autowired
	private GetTrainingPlanUseCase getTrainingPlanUseCase;

	@Autowired
	private GetWorkoutOccurrenceUseCase getWorkoutOccurrenceUseCase;

	@Autowired
	private StartWorkoutOccurrenceUseCase startWorkoutOccurrenceUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private UpdateWorkoutExerciseSetUseCase updateWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseSetUseCase completeWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;

	@Autowired
	private CompleteWorkoutOccurrenceUseCase completeWorkoutOccurrenceUseCase;

	@Autowired
	private GetWorkoutOccurrenceLoadSummaryUseCase getWorkoutOccurrenceLoadSummaryUseCase;

	@Autowired
	private GetWorkoutOccurrencePerformanceSummaryUseCase getWorkoutOccurrencePerformanceSummaryUseCase;

	@Autowired
	private SubmitWorkoutSessionEffortUseCase submitWorkoutSessionEffortUseCase;

	@Autowired
	private GetWorkoutSessionEffortUseCase getWorkoutSessionEffortUseCase;

	@Autowired
	private DailyAthleteStateSnapshotRepository snapshotRepository;

	@Autowired
	private DailyReadinessAssessmentRepository readinessRepository;

	@Autowired
	private DailyTrainingRecommendationRepository recommendationRepository;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	@Test
	void releaseCandidateCrossFeaturePathCoversIsolationRecoveryAdaptationExecutionAndBudgets() {
		// RC11: empty today before any generation — present=false, no hidden writes.
		AccountId athleteA = athlete("Alex", "River");
		AthleteId athleteAId = athleteId(athleteA);
		TrainingTodayDashboardResult emptyToday = getTrainingTodayDashboardUseCase.execute(athleteA, JULY_31);
		assertThat(emptyToday.recovery().checkInPresent()).isFalse();
		assertThat(emptyToday.athleteState().snapshotPresent()).isFalse();
		assertThat(emptyToday.readiness().readinessPresent()).isFalse();
		assertThat(emptyToday.recommendation().recommendationPresent()).isFalse();
		assertThat(emptyToday.training().primaryOccurrence()).isNull();
		assertThat(emptyToday.adaptation().activeProposalPresent()).isFalse();
		assertThat(snapshotRepository.findCurrentByAthleteIdAndStateDate(athleteAId, JULY_31)).isEmpty();
		assertThat(readinessRepository.findHistory(athleteAId, JULY_31, JULY_31, true, null, 0, 10)).isEmpty();
		assertThat(recommendationRepository.findHistory(athleteAId, JULY_31, JULY_31, true, null, null, 0, 10))
				.isEmpty();

		// RC03 setup: environment + plan/day/exercises + occurrence (mixed equipment → partial feasibility).
		seedPriorCheckIns(athleteA);
		ScheduledWorkout scheduled = schedulePartiallyFeasibleBenchDay(athleteA, JULY_31);

		// RC06: environment + feasibility.
		WorkoutOccurrenceFeasibilityResult feasibility = analyzeWorkoutOccurrenceFeasibilityUseCase.execute(
				athleteA, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId(), 3, true);
		assertThat(feasibility.summary().status()).isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);

		// RC07: recovery check-in → snapshot → readiness → recommendation.
		createDailyRecoveryCheckInUseCase.execute(
				athleteA, JULY_31, 360, 3, 5, 5, 2, 3, 3,
				List.of(new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 2, null)), null);
		DailyAthleteStateSnapshotResult snapshot = generateDailyAthleteStateSnapshotUseCase.execute(
				athleteA, JULY_31, BASELINE_WINDOW);
		DailyReadinessAssessmentResult readiness = generateDailyReadinessAssessmentUseCase.execute(
				athleteA, snapshot.snapshotId());
		DailyTrainingRecommendationResult recommendation = generateDailyTrainingRecommendationUseCase.execute(
				athleteA, readiness.assessmentId());
		assertThat(recommendation.overallAction()).isNotNull();

		// RC08: recommendation-origin adaptation when MODIFY_SESSION (apply deferred to dedicated suite).
		UUID proposalId = null;
		if (recommendation.overallAction() == TrainingRecommendationAction.MODIFY_SESSION) {
			WorkoutAdaptationProposalResult proposal = generateRecommendedAdaptationUseCase.execute(
					athleteA,
					recommendation.recommendationId(),
					scheduled.occurrenceId().value(),
					3,
					true,
					30);
			assertThat(proposal.origin()).isEqualTo(WorkoutAdaptationProposalOrigin.TRAINING_RECOMMENDATION);
			proposalId = proposal.id().value();
		}

		// RC10 + facade query budgets while occurrence is still SCHEDULED.
		SessionFactory sessionFactory = entityManagerFactory.unwrap(SessionFactory.class);
		sessionFactory.getStatistics().setStatisticsEnabled(true);

		sessionFactory.getStatistics().clear();
		TrainingTodayDashboardResult today = getTrainingTodayDashboardUseCase.execute(athleteA, JULY_31);
		assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(17);
		assertThat(today.recovery().checkInPresent()).isTrue();
		assertThat(today.athleteState().snapshotPresent()).isTrue();
		assertThat(today.readiness().readinessPresent()).isTrue();
		assertThat(today.recommendation().recommendationPresent()).isTrue();
		assertThat(today.training().primaryOccurrence()).isNotNull();
		assertThat(today.training().primaryOccurrence().occurrenceId()).isEqualTo(scheduled.occurrenceId().value());
		assertThat(today.training().primaryOccurrence().feasibilityStatus())
				.isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);
		if (proposalId != null) {
			assertThat(today.adaptation().activeProposalPresent()).isTrue();
			assertThat(today.adaptation().adaptationProposalId()).isEqualTo(proposalId);
		}

		sessionFactory.getStatistics().clear();
		WorkoutLaunchContextResult launch = getWorkoutLaunchContextUseCase.execute(
				athleteA, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId());
		assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(15);
		assertThat(launch.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.SCHEDULED);
		assertThat(launch.feasibility().status()).isEqualTo(WorkoutFeasibilityStatus.PARTIALLY_FEASIBLE);

		sessionFactory.getStatistics().clear();
		TrainingOverviewResult overview = getTrainingOverviewUseCase.execute(athleteA, JULY_31);
		assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(15);
		assertThat(overview.activePlans()).isNotEmpty();
		assertThat(overview.upcomingOccurrences()).isNotEmpty();

		sessionFactory.getStatistics().clear();
		RecoveryOverviewResult recovery = getRecoveryOverviewUseCase.execute(athleteA, JULY_31, 7);
		assertThat(sessionFactory.getStatistics().getPrepareStatementCount()).isLessThanOrEqualTo(12);
		assertThat(recovery.checkInPresent()).isTrue();
		assertThat(recovery.readinessPresent()).isTrue();
		assertThat(recovery.recommendationPresent()).isTrue();

		// RC02: athlete B cannot read A's plan/occurrence; dashboard stays empty for B.
		AccountId athleteB = athlete("Blake", "Stone");
		assertThatThrownBy(() -> getTrainingPlanUseCase.execute(athleteB, scheduled.planId()))
				.isInstanceOf(TrainingPlanNotFoundException.class);
		// Nested ownership fails at the plan boundary first (no existence leak of day/occurrence).
		assertThatThrownBy(() -> getWorkoutOccurrenceUseCase.execute(
				athleteB, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId()))
				.isInstanceOf(TrainingPlanNotFoundException.class);
		TrainingTodayDashboardResult foreignToday = getTrainingTodayDashboardUseCase.execute(athleteB, JULY_31);
		assertThat(foreignToday.training().primaryOccurrence()).isNull();
		assertThat(foreignToday.recovery().checkInPresent()).isFalse();
		assertThat(foreignToday.recommendation().recommendationPresent()).isFalse();

		// RC15: sanitized application exception without HTTP (invalid client date).
		assertThatThrownBy(() -> getTrainingTodayDashboardUseCase.execute(
				athleteA, LocalDate.of(2099, 1, 1)))
				.isInstanceOf(InvalidTrainingClientDateException.class);

		// RC03–RC05: start → log a set → complete execution → complete occurrence → load/performance path.
		WorkoutOccurrenceDetailResult started = startWorkoutOccurrenceUseCase.execute(
				athleteA, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId());
		assertThat(started.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.IN_PROGRESS);

		for (WorkoutExerciseExecutionResult execution : started.executions()) {
			List<WorkoutExerciseSetResult> sets = listWorkoutExerciseSetsUseCase.execute(
					athleteA, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId(), execution.id());
			WorkoutExerciseSetResult first = sets.getFirst();
			if (execution.prescribedExerciseDefinitionId().equals(SystemExerciseDefinitions.BENCH_PRESS)) {
				updateWorkoutExerciseSetUseCase.execute(
						athleteA,
						scheduled.planId(),
						scheduled.dayId(),
						scheduled.occurrenceId(),
						execution.id(),
						first.id(),
						new UpdateWorkoutExerciseSetCommand(
								null, false,
								5, true,
								new BigDecimal("80"), true,
								WeightUnit.KILOGRAM, true,
								null, false,
								null, false,
								null, false,
								null, false,
								null, false,
								null, false));
			}
			for (WorkoutExerciseSetResult set : sets) {
				completeWorkoutExerciseSetUseCase.execute(
						athleteA,
						scheduled.planId(),
						scheduled.dayId(),
						scheduled.occurrenceId(),
						execution.id(),
						set.id());
			}
			completeWorkoutExerciseExecutionUseCase.execute(
					athleteA, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId(), execution.id());
		}

		WorkoutOccurrenceDetailResult completed = completeWorkoutOccurrenceUseCase.execute(
				athleteA, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId());
		assertThat(completed.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.COMPLETED);

		WorkoutOccurrenceLoadSummaryResult load = getWorkoutOccurrenceLoadSummaryUseCase.execute(
				athleteA, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId());
		assertThat(load.completedExerciseCount()).isEqualTo(started.executions().size());
		WorkoutOccurrencePerformanceResult performance = getWorkoutOccurrencePerformanceSummaryUseCase.execute(
				athleteA, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId());
		assertThat(performance.exercises()).hasSize(started.executions().size());

		// RC09: session effort after complete.
		submitWorkoutSessionEffortUseCase.execute(
				athleteA,
				scheduled.planId(),
				scheduled.dayId(),
				scheduled.occurrenceId(),
				new BigDecimal("7.0"),
				45,
				"RC smoke");
		WorkoutSessionEffortResult effort = getWorkoutSessionEffortUseCase.execute(
				athleteA, scheduled.planId(), scheduled.dayId(), scheduled.occurrenceId());
		assertThat(effort.sessionRpe().value()).isEqualByComparingTo("7.0");
	}

	private ScheduledWorkout schedulePartiallyFeasibleBenchDay(AccountId accountId, LocalDate date) {
		TrainingEnvironmentResult environment = createTrainingEnvironmentUseCase.execute(
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

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "RC " + UUID.randomUUID(), null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null,
				environment.id().value());
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
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), occurrence.occurrence().id(), environment.id());
		return new ScheduledWorkout(plan.id(), day.id(), occurrence.occurrence().id());
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

	private AccountId athlete(String firstName, String lastName) {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				firstName, lastName, LocalDate.of(1993, 4, 11), Sex.MALE,
				Height.ofCentimeters(180), Weight.ofKilograms(82),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		return accountId;
	}

	private AthleteId athleteId(AccountId accountId) {
		return AthleteId.of(athleteContextPort.requireAthlete(accountId.value()).athleteId());
	}

	private record ScheduledWorkout(
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId) {
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
