package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsReasonCode;
import com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency;
import com.devinolabs.uap.training.domain.RecoveryComparisonBand;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.RecoveryTrendDirection;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@Import({ TestcontainersConfiguration.class, RecoveryBaselineAcceptanceIntegrationTests.MutableClockConfig.class })
class RecoveryBaselineAcceptanceIntegrationTests {

	private static final LocalDate JULY_31 = LocalDate.of(2026, 7, 31);
	private static final int BASELINE_WINDOW = 7;

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
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private UpdateWorkoutExerciseSetUseCase updateWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseSetUseCase completeWorkoutExerciseSetUseCase;

	@Autowired
	private SkipWorkoutExerciseSetUseCase skipWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;

	@Autowired
	private CompleteWorkoutOccurrenceUseCase completeWorkoutOccurrenceUseCase;

	@Autowired
	private SubstituteWorkoutExerciseExecutionUseCase substituteWorkoutExerciseExecutionUseCase;

	@Autowired
	private SubmitWorkoutSessionEffortUseCase submitWorkoutSessionEffortUseCase;

	@Autowired
	private UpdateWorkoutSessionEffortUseCase updateWorkoutSessionEffortUseCase;

	@Autowired
	private CreateDailyRecoveryCheckInUseCase createDailyRecoveryCheckInUseCase;

	@Autowired
	private UpdateDailyRecoveryCheckInUseCase updateDailyRecoveryCheckInUseCase;

	@Autowired
	private ListDailyRecoveryCheckInRevisionsUseCase listDailyRecoveryCheckInRevisionsUseCase;

	@Autowired
	private CompareDailyRecoveryCheckInToBaselineUseCase compareDailyRecoveryCheckInToBaselineUseCase;

	@Autowired
	private CompareRecoveryDateToBaselineUseCase compareRecoveryDateToBaselineUseCase;

	@Autowired
	private GetRecoveryMetricTrendUseCase getRecoveryMetricTrendUseCase;

	@Autowired
	private GetRecoveryBaselineDashboardUseCase getRecoveryBaselineDashboardUseCase;

	@Autowired
	private GetDailyRecoveryCheckInByDateUseCase getDailyRecoveryCheckInByDateUseCase;

	@Test
	void criticalRecoveryBaselineScenarioWithTrainingLoadAndHistoricalCorrection() {
		AccountId accountId = athlete();
		seedPriorCheckIns(accountId);
		completeMixedSession(accountId, prescribeMixedSession(accountId), JULY_31);

		DailyRecoveryCheckInResult target = createDailyRecoveryCheckInUseCase.execute(
				accountId,
				JULY_31,
				360,
				2,
				5,
				4,
				4,
				2,
				2,
				null,
				null);

		DailyRecoveryBaselineComparisonResult comparison = compareDailyRecoveryCheckInToBaselineUseCase.execute(
				accountId,
				target.id(),
				BASELINE_WINDOW,
				true);

		assertThat(comparison.checkInPresent()).isTrue();
		assertThat(comparison.baselineWindowDays()).isEqualTo(BASELINE_WINDOW);
		assertThat(comparison.metricComparisons()).hasSize(RecoveryMetricType.values().length);
		assertThat(comparison.trainingLoadContext().totalVolumeKilograms()).isEqualByComparingTo("2400");
		assertThat(comparison.trainingLoadContext().totalDistanceMeters()).isEqualByComparingTo("5000");
		assertThat(comparison.trainingLoadContext().totalDurationSeconds()).isEqualTo(1920L);
		assertThat(comparison.trainingLoadContext().totalSessionRpeLoad()).isEqualByComparingTo("552.50");

		RecoveryMetricDeviationResult fatigueBeforeCorrection = metric(comparison, RecoveryMetricType.FATIGUE);
		assertThat(fatigueBeforeCorrection.baseline().observationCount()).isEqualTo(7);
		assertThat(fatigueBeforeCorrection.baseline().dataSufficiency())
				.isEqualTo(RecoveryBaselineDataSufficiency.SUFFICIENT);
		assertThat(fatigueBeforeCorrection.baseline().mean()).isEqualByComparingTo("3.14");
		assertThat(fatigueBeforeCorrection.targetValue()).isEqualByComparingTo("5");
		assertThat(fatigueBeforeCorrection.comparisonBand()).isEqualTo(RecoveryComparisonBand.FAR_ABOVE_BASELINE);
		assertThat(fatigueBeforeCorrection.reasonCode()).isEqualTo(RecoveryAnalyticsReasonCode.BASELINE_AVAILABLE);

		RecoveryMetricDeviationResult sleep = metric(comparison, RecoveryMetricType.SLEEP_DURATION);
		assertThat(sleep.baseline().observationCount()).isEqualTo(7);
		assertThat(sleep.percentageDifference()).isNotNull();
		assertThat(sleep.percentageDifference()).isNegative();

		DailyRecoveryCheckInResult july26 = findCheckInByDate(accountId, LocalDate.of(2026, 7, 26));
		updateDailyRecoveryCheckInUseCase.execute(
				accountId,
				july26.id(),
				new UpdateDailyRecoveryCheckInCommand(
						null, false,
						null, false,
						2, true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null));

		DailyRecoveryBaselineComparisonResult afterCorrection = compareDailyRecoveryCheckInToBaselineUseCase.execute(
				accountId,
				target.id(),
				BASELINE_WINDOW,
				false);
		RecoveryMetricDeviationResult fatigueAfterCorrection = metric(afterCorrection, RecoveryMetricType.FATIGUE);
		assertThat(fatigueAfterCorrection.baseline().observationCount()).isEqualTo(7);
		assertThat(fatigueAfterCorrection.baseline().mean()).isEqualByComparingTo("2.86");
		assertThat(listDailyRecoveryCheckInRevisionsUseCase.execute(accountId, july26.id())).hasSize(1);

		DailyRecoveryBaselineComparisonResult isolatedComparison =
				compareRecoveryDateToBaselineUseCase.execute(
						accountId,
						JULY_31,
						BASELINE_WINDOW,
						false);
		assertThat(isolatedComparison.metricComparisons()).hasSize(RecoveryMetricType.values().length);
		assertThat(metric(isolatedComparison, RecoveryMetricType.FATIGUE).baseline().mean())
				.isEqualByComparingTo("2.86");

		createDailyRecoveryCheckInUseCase.execute(
				accountId,
				LocalDate.of(2026, 8, 1),
				420,
				3,
				3,
				3,
				3,
				4,
				4,
				null,
				null);
		DailyRecoveryBaselineComparisonResult afterLaterCheckIn = compareDailyRecoveryCheckInToBaselineUseCase.execute(
				accountId,
				target.id(),
				BASELINE_WINDOW,
				false);
		assertThat(metric(afterLaterCheckIn, RecoveryMetricType.FATIGUE).baseline().mean())
				.isEqualByComparingTo("2.86");

		LocalDate trendStart = LocalDate.of(2026, 7, 24);
		RecoveryMetricTrendResult fatigueTrend = getRecoveryMetricTrendUseCase.execute(
				accountId,
				RecoveryMetricType.FATIGUE,
				trendStart,
				JULY_31,
				true);
		assertThat(fatigueTrend.observationCount()).isEqualTo(8);
		assertThat(fatigueTrend.points()).hasSize(8);
		assertThat(fatigueTrend.points().getFirst().date()).isEqualTo(trendStart);
		assertThat(fatigueTrend.points().getLast().date()).isEqualTo(JULY_31);
		assertThat(fatigueTrend.points().get(2).rollingAverage3()).isNotNull();
		assertThat(fatigueTrend.trendDirection()).isNotEqualTo(RecoveryTrendDirection.INSUFFICIENT_DATA);
		assertThat(fatigueTrend.points().stream().map(RecoveryMetricTrendPointResult::trainingLoadContext))
				.anyMatch(context -> context != null && context.totalVolumeKilograms().compareTo(BigDecimal.ZERO) > 0);

		RecoveryBaselineDashboardResult dashboard = getRecoveryBaselineDashboardUseCase.execute(
				accountId,
				JULY_31,
				BASELINE_WINDOW,
				true);
		assertThat(dashboard.checkInPresent()).isTrue();
		assertThat(dashboard.baselines()).hasSize(RecoveryMetricType.values().length);
		assertThat(dashboard.metricDeviations()).hasSize(RecoveryMetricType.values().length);
		assertThat(dashboard.metricTrends()).hasSize(RecoveryMetricType.values().length);
	}

	@Test
	void missingCheckInDateComparisonReturnsCheckInAbsent() {
		AccountId accountId = athlete();
		DailyRecoveryBaselineComparisonResult comparison = compareRecoveryDateToBaselineUseCase.execute(
				accountId,
				LocalDate.of(2026, 7, 20),
				BASELINE_WINDOW,
				false);
		assertThat(comparison.checkInPresent()).isFalse();
		assertThat(comparison.metricComparisons()).hasSize(RecoveryMetricType.values().length);
		assertThat(comparison.metricComparisons().stream()
				.allMatch(entry -> entry.reasonCode() == RecoveryAnalyticsReasonCode.TARGET_VALUE_MISSING))
				.isTrue();
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
				accountId,
				date,
				sleepMinutes,
				sleepQuality,
				fatigue,
				soreness,
				stress,
				mood,
				motivation,
				null,
				null);
	}

	private DailyRecoveryCheckInResult findCheckInByDate(AccountId accountId, LocalDate date) {
		return getDailyRecoveryCheckInByDateUseCase.execute(accountId, date);
	}

	private static RecoveryMetricDeviationResult metric(
			DailyRecoveryBaselineComparisonResult comparison,
			RecoveryMetricType metricType) {
		return comparison.metricComparisons().stream()
				.filter(entry -> entry.metricType() == metricType)
				.findFirst()
				.orElseThrow();
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Jordan", "Reed", LocalDate.of(1994, 3, 14), Sex.FEMALE,
				Height.ofCentimeters(168), Weight.ofKilograms(64),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		return accountId;
	}

	private void completeMixedSession(AccountId accountId, Prescription prescription, LocalDate scheduledDate) {
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), scheduledDate, null, null);

		WorkoutExerciseExecutionId backSquatExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.BACK_SQUAT).id();
		WorkoutExerciseExecutionId frontSquatExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.FRONT_SQUAT).id();
		WorkoutExerciseExecutionId runningExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.RUNNING).id();
		WorkoutExerciseExecutionId plankExecutionId = executionFor(
				occurrence, SystemExerciseDefinitions.PLANK).id();

		substituteWorkoutExerciseExecutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id(),
				frontSquatExecutionId, SystemExerciseDefinitions.GOBLET_SQUAT,
				ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE, "No rack", null);

		completeStrengthExercise(accountId, prescription, occurrence.occurrence().id(), backSquatExecutionId,
				new BigDecimal("100"), 5, 3);
		completeStrengthExercise(accountId, prescription, occurrence.occurrence().id(), frontSquatExecutionId,
				new BigDecimal("30"), 10, 3);
		completeDistanceExercise(accountId, prescription, occurrence.occurrence().id(), runningExecutionId,
				new BigDecimal("5000"), 1800);
		completeDurationExercise(accountId, prescription, occurrence.occurrence().id(), plankExecutionId, 120);

		completeWorkoutOccurrenceUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id());

		submitWorkoutSessionEffortUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id(),
				new BigDecimal("8.0"), 60, "Hard but manageable");
		updateWorkoutSessionEffortUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrence.occurrence().id(),
				new BigDecimal("8.5"), 65, "Felt heavier on review");
	}

	private Prescription prescribeMixedSession(AccountId accountId) {
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Recovery Load Block", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Mixed", null, 1, DayOfWeek.FRIDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BACK_SQUAT, "Back Squat",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, 0);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.FRONT_SQUAT, "Front Squat",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 10, 10, new BigDecimal("30"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, 1);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.RUNNING, "Running",
				ExerciseCategory.CARDIO, ExerciseType.RUN,
				1, null, null, null, null,
				null, null, null, null, null, null, null, 2);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.PLANK, "Plank",
				ExerciseCategory.STRENGTH, ExerciseType.BODYWEIGHT,
				1, null, null, null, null,
				120, null, null, null, null, null, null, 3);
		return new Prescription(accountId, plan.id(), day.id());
	}

	private static WorkoutExerciseExecutionResult executionFor(
			WorkoutOccurrenceDetailResult occurrence,
			ExerciseDefinitionId prescribedDefinitionId) {
		return occurrence.executions().stream()
				.filter(execution -> execution.prescribedExerciseDefinitionId().equals(prescribedDefinitionId))
				.findFirst()
				.orElseThrow();
	}

	private void completeStrengthExercise(
			AccountId accountId,
			Prescription prescription,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			BigDecimal weight,
			int reps,
			int completedSetCount) {
		List<WorkoutExerciseSetResult> sets = listWorkoutExerciseSetsUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId);
		for (int index = 0; index < completedSetCount; index++) {
			WorkoutExerciseSetResult set = sets.get(index);
			updateWorkoutExerciseSetUseCase.execute(
					accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id(),
					new UpdateWorkoutExerciseSetCommand(
							null, false, reps, true, weight, true, WeightUnit.KILOGRAM, true,
							null, false, null, false, null, false, null, false, null, false, null, false));
			completeWorkoutExerciseSetUseCase.execute(
					accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id());
		}
		sets.stream().skip(completedSetCount).forEach(set -> skipWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id()));
		completeWorkoutExerciseExecutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId);
	}

	private void completeDistanceExercise(
			AccountId accountId,
			Prescription prescription,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			BigDecimal distanceMeters,
			int durationSeconds) {
		WorkoutExerciseSetResult set = listWorkoutExerciseSetsUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId).getFirst();
		updateWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id(),
				new UpdateWorkoutExerciseSetCommand(
						null, false, null, false, null, false, null, false,
						durationSeconds, true, distanceMeters, true, DistanceUnit.METER, true,
						null, false, null, false, null, false));
		completeWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id());
		completeWorkoutExerciseExecutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId);
	}

	private void completeDurationExercise(
			AccountId accountId,
			Prescription prescription,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			int durationSeconds) {
		WorkoutExerciseSetResult set = listWorkoutExerciseSetsUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId).getFirst();
		updateWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id(),
				new UpdateWorkoutExerciseSetCommand(
						null, false, null, false, null, false, null, false,
						durationSeconds, true, null, false, null, false,
						null, false, null, false, null, false));
		completeWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId, set.id());
		completeWorkoutExerciseExecutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId, executionId);
	}

	@TestConfiguration
	static class MutableClockConfig {

		@Bean
		@Primary
		Clock mutableClock() {
			return Clock.fixed(Instant.parse("2026-08-01T12:00:00Z"), ZoneOffset.UTC);
		}

	}

	private record Prescription(AccountId accountId, TrainingPlanId planId, WorkoutDayId dayId) {
	}

}
