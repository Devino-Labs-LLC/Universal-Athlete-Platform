package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.devinolabs.uap.training.domain.DailyAthleteStateCompleteness;
import com.devinolabs.uap.training.domain.DailyAthleteStateGenerationReason;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.RecoveryAnalyticsCalculationVersion;
import com.devinolabs.uap.training.domain.RecoveryBaselineDataSufficiency;
import com.devinolabs.uap.training.domain.RecoveryComparisonBand;
import com.devinolabs.uap.training.domain.RecoveryMetricType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@Import({ TestcontainersConfiguration.class, DailyAthleteStateAcceptanceIntegrationTests.MutableClockConfig.class })
class DailyAthleteStateAcceptanceIntegrationTests {

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
	private GetDailyRecoveryCheckInByDateUseCase getDailyRecoveryCheckInByDateUseCase;

	@Autowired
	private GenerateDailyAthleteStateSnapshotUseCase generateDailyAthleteStateSnapshotUseCase;

	@Autowired
	private RegenerateDailyAthleteStateSnapshotUseCase regenerateDailyAthleteStateSnapshotUseCase;

	@Autowired
	private GetDailyAthleteStateSnapshotUseCase getDailyAthleteStateSnapshotUseCase;

	@Autowired
	private ListDailyAthleteStateSnapshotVersionsUseCase listDailyAthleteStateSnapshotVersionsUseCase;

	@Autowired
	private CompareDailyAthleteStateSnapshotsUseCase compareDailyAthleteStateSnapshotsUseCase;

	@Autowired
	private RecomputeWorkoutOccurrenceLoadUseCase recomputeWorkoutOccurrenceLoadUseCase;

	@Test
	void criticalDailyAthleteStateSnapshotScenario() {
		AccountId accountId = athlete();
		seedPriorCheckIns(accountId);
		Prescription prescription = prescribeMixedSession(accountId);
		WorkoutOccurrenceId occurrenceId = completeMixedSession(accountId, prescription, JULY_31);

		createDailyRecoveryCheckInUseCase.execute(
				accountId, JULY_31, 360, 2, 5, 4, 4, 2, 2, null, null);

		DailyAthleteStateSnapshotResult v1 = generateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		assertThat(v1.snapshotVersion()).isEqualTo(1);
		assertThat(v1.current()).isTrue();
		assertThat(v1.changed()).isTrue();
		assertThat(v1.generationReason()).isEqualTo(DailyAthleteStateGenerationReason.MANUAL);
		assertThat(v1.completeness()).isEqualTo(DailyAthleteStateCompleteness.COMPLETE);
		assertThat(v1.recoveryAnalyticsCalculationVersion())
				.isEqualTo(RecoveryAnalyticsCalculationVersion.RECOVERY_ANALYTICS_V1);
		assertThat(v1.checkInPresent()).isTrue();
		assertThat(v1.fatigue()).isEqualTo(5);
		assertThat(v1.recoveryMetrics()).hasSize(RecoveryMetricType.values().length);
		assertThat(metric(v1, RecoveryMetricType.FATIGUE).observationCount()).isEqualTo(7);
		assertThat(metric(v1, RecoveryMetricType.FATIGUE).dataSufficiency())
				.isEqualTo(RecoveryBaselineDataSufficiency.SUFFICIENT);
		assertThat(metric(v1, RecoveryMetricType.FATIGUE).baselineMean()).isEqualByComparingTo("3.14");
		assertThat(metric(v1, RecoveryMetricType.FATIGUE).comparisonBand())
				.isEqualTo(RecoveryComparisonBand.FAR_ABOVE_BASELINE);
		assertThat(v1.totalVolumeKilograms()).isEqualByComparingTo("2400");
		assertThat(v1.totalDistanceMeters()).isEqualByComparingTo("5000");
		assertThat(v1.totalDurationSeconds()).isEqualTo(1920L);
		assertThat(v1.totalSessionRpeLoad()).isEqualByComparingTo("552.50");
		assertThat(v1.occurrenceCount()).isEqualTo(1);
		assertThat(v1.completedOccurrenceCount()).isEqualTo(1);
		assertThat(v1.scheduledOccurrenceCount()).isEqualTo(1);
		assertThat(v1.completedScheduledCount()).isEqualTo(1);

		UUID v1Id = v1.snapshotId();
		String v1Fingerprint = v1.sourceFingerprint();

		DailyAthleteStateSnapshotResult unchanged = generateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		assertThat(unchanged.snapshotId()).isEqualTo(v1Id);
		assertThat(unchanged.snapshotVersion()).isEqualTo(1);
		assertThat(unchanged.changed()).isFalse();
		assertThat(unchanged.sourceFingerprint()).isEqualTo(v1Fingerprint);

		DailyRecoveryCheckInResult july26 = getDailyRecoveryCheckInByDateUseCase.execute(
				accountId, LocalDate.of(2026, 7, 26));
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

		DailyAthleteStateSnapshotResult stillV1 = getDailyAthleteStateSnapshotUseCase.execute(accountId, v1Id);
		assertThat(stillV1.snapshotVersion()).isEqualTo(1);
		assertThat(metric(stillV1, RecoveryMetricType.FATIGUE).baselineMean()).isEqualByComparingTo("3.14");

		DailyAthleteStateSnapshotResult v2 = regenerateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		assertThat(v2.changed()).isTrue();
		assertThat(v2.snapshotVersion()).isEqualTo(2);
		assertThat(v2.current()).isTrue();
		assertThat(v2.generationReason()).isEqualTo(DailyAthleteStateGenerationReason.REGENERATED);
		assertThat(v2.sourceFingerprint()).isNotEqualTo(v1Fingerprint);
		assertThat(metric(v2, RecoveryMetricType.FATIGUE).observationCount()).isEqualTo(7);
		assertThat(metric(v2, RecoveryMetricType.FATIGUE).baselineMean()).isEqualByComparingTo("2.86");

		DailyAthleteStateSnapshotResult historicalV1 = getDailyAthleteStateSnapshotUseCase.execute(accountId, v1Id);
		assertThat(historicalV1.current()).isFalse();
		assertThat(metric(historicalV1, RecoveryMetricType.FATIGUE).baselineMean()).isEqualByComparingTo("3.14");

		createDailyRecoveryCheckInUseCase.execute(
				accountId, LocalDate.of(2026, 8, 1), 420, 3, 3, 3, 3, 4, 4, null, null);
		DailyAthleteStateSnapshotResult afterLater = regenerateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		assertThat(afterLater.changed()).isFalse();
		assertThat(afterLater.snapshotVersion()).isEqualTo(2);
		assertThat(afterLater.snapshotId()).isEqualTo(v2.snapshotId());

		updateWorkoutSessionEffortUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId,
				new BigDecimal("9.0"), 70, "Corrected objective-adjacent effort after review");
		recomputeWorkoutOccurrenceLoadUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), occurrenceId);

		DailyAthleteStateSnapshotResult v3 = regenerateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		assertThat(v3.changed()).isTrue();
		assertThat(v3.snapshotVersion()).isEqualTo(3);
		assertThat(v3.current()).isTrue();
		assertThat(v3.totalSessionRpeLoad()).isNotEqualByComparingTo("552.50");

		DailyAthleteStateSnapshotComparisonResult comparison = compareDailyAthleteStateSnapshotsUseCase.execute(
				accountId, v1Id, v3.snapshotId());
		assertThat(comparison.baselineChanged()).isTrue();
		assertThat(comparison.trainingLoadChanged()).isTrue();
		assertThat(comparison.fieldDifferences()).isNotEmpty();

		List<DailyAthleteStateSnapshotSummary> versions =
				listDailyAthleteStateSnapshotVersionsUseCase.execute(accountId, JULY_31);
		assertThat(versions).hasSize(3);
		assertThat(versions.getFirst().snapshotVersion()).isEqualTo(3);
		assertThat(versions.getFirst().current()).isTrue();
		assertThat(versions.stream().filter(DailyAthleteStateSnapshotSummary::current).count()).isEqualTo(1);
	}

	private static com.devinolabs.uap.training.domain.DailyAthleteStateRecoveryMetricSnapshot metric(
			DailyAthleteStateSnapshotResult snapshot,
			RecoveryMetricType metricType) {
		return snapshot.recoveryMetrics().stream()
				.filter(entry -> entry.metricType() == metricType)
				.findFirst()
				.orElseThrow();
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
				"Jordan", "Reed", LocalDate.of(1994, 3, 14), Sex.FEMALE,
				Height.ofCentimeters(168), Weight.ofKilograms(64),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		return accountId;
	}

	private WorkoutOccurrenceId completeMixedSession(
			AccountId accountId,
			Prescription prescription,
			LocalDate scheduledDate) {
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
		return occurrence.occurrence().id();
	}

	private Prescription prescribeMixedSession(AccountId accountId) {
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Athlete State Block", null,
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
