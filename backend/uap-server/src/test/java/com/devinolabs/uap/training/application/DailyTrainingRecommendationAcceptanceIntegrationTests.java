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
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.ReadinessBand;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationStatus;
import com.devinolabs.uap.training.domain.WeightUnit;

@SpringBootTest
@Import({ TestcontainersConfiguration.class, DailyTrainingRecommendationAcceptanceIntegrationTests.MutableClockConfig.class })
class DailyTrainingRecommendationAcceptanceIntegrationTests {

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
	private GenerateDailyReadinessAssessmentUseCase generateDailyReadinessAssessmentUseCase;

	@Autowired
	private GenerateCurrentDailyReadinessAssessmentUseCase generateCurrentDailyReadinessAssessmentUseCase;

	@Autowired
	private GenerateDailyTrainingRecommendationUseCase generateDailyTrainingRecommendationUseCase;

	@Autowired
	private GenerateCurrentDailyTrainingRecommendationUseCase generateCurrentDailyTrainingRecommendationUseCase;

	@Autowired
	private GetDailyTrainingRecommendationUseCase getDailyTrainingRecommendationUseCase;

	@Autowired
	private CompareDailyTrainingRecommendationsUseCase compareDailyTrainingRecommendationsUseCase;

	@Test
	void criticalRecommendationFromLowReadinessAndScheduledWorkout() {
		AccountId accountId = athlete();
		seedPriorCheckIns(accountId);
		scheduleLowerBody(accountId, JULY_31);

		createDailyRecoveryCheckInUseCase.execute(
				accountId,
				JULY_31,
				360,
				2,
				5,
				4,
				4,
				2,
				2,
				List.of(new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 2, "Mild tightness")),
				null);

		DailyAthleteStateSnapshotResult snapshot = generateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		DailyReadinessAssessmentResult readiness = generateDailyReadinessAssessmentUseCase.execute(
				accountId, snapshot.snapshotId());
		assertThat(readiness.readinessBand()).isEqualTo(ReadinessBand.LOW);
		assertThat(readiness.readinessScore()).isNotNull();
		assertThat(readiness.limitingDimensions()).isNotEmpty();

		DailyTrainingRecommendationResult recommendation = generateDailyTrainingRecommendationUseCase.execute(
				accountId, readiness.assessmentId());
		assertThat(recommendation.newlyCreated()).isTrue();
		assertThat(recommendation.overallAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
		assertThat(recommendation.recommendationStatus()).isEqualTo(TrainingRecommendationStatus.ACTIONABLE);
		assertThat(recommendation.dailyReadinessAssessmentId()).isEqualTo(readiness.assessmentId());
		assertThat(recommendation.dailyAthleteStateSnapshotVersion()).isEqualTo(snapshot.snapshotVersion());
		assertThat(recommendation.adjustments()).extracting(a -> a.type())
				.contains(
						TrainingAdjustmentType.REDUCE_INTENSITY,
						TrainingAdjustmentType.REDUCE_TOTAL_VOLUME);
		assertThat(recommendation.adjustments()).extracting(a -> a.type())
				.doesNotHaveDuplicates();
		assertThat(recommendation.modifiableScheduledOccurrenceCount()).isEqualTo(1);
		assertThat(recommendation.scheduledOccurrences()).hasSize(1);
		assertThat(recommendation.limitingDimensions()).isEqualTo(readiness.limitingDimensions());

		UUID recommendationId = recommendation.recommendationId();
		DailyTrainingRecommendationResult duplicate = generateDailyTrainingRecommendationUseCase.execute(
				accountId, readiness.assessmentId());
		assertThat(duplicate.newlyCreated()).isFalse();
		assertThat(duplicate.recommendationId()).isEqualTo(recommendationId);

		DailyRecoveryCheckInResult july26 = getDailyRecoveryCheckInByDateUseCase.execute(
				accountId, LocalDate.of(2026, 7, 26));
		updateDailyRecoveryCheckInUseCase.execute(
				accountId,
				july26.id(),
				new UpdateDailyRecoveryCheckInCommand(
						null, false, null, false, 2, true,
						null, false, null, false, null, false, null, false, null, false, null, false, null));

		DailyTrainingRecommendationResult stillSame = generateDailyTrainingRecommendationUseCase.execute(
				accountId, readiness.assessmentId());
		assertThat(stillSame.recommendationId()).isEqualTo(recommendationId);
		assertThat(stillSame.overallAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);

		updateDailyRecoveryCheckInUseCase.execute(
				accountId,
				getDailyRecoveryCheckInByDateUseCase.execute(accountId, JULY_31).id(),
				new UpdateDailyRecoveryCheckInCommand(
						480, true, 4, true, 2, true, 2, true, 2, true, 4, true, 4, true,
						List.of(), true, null, false, null));

		DailyAthleteStateSnapshotResult snapshotV2 = regenerateDailyAthleteStateSnapshotUseCase.execute(
				accountId, JULY_31, BASELINE_WINDOW);
		assertThat(snapshotV2.changed()).isTrue();
		DailyReadinessAssessmentResult readinessV2 = generateCurrentDailyReadinessAssessmentUseCase.execute(
				accountId, JULY_31);
		assertThat(readinessV2.assessmentId()).isNotEqualTo(readiness.assessmentId());

		DailyTrainingRecommendationResult recommendationV2 =
				generateCurrentDailyTrainingRecommendationUseCase.execute(accountId, JULY_31);
		assertThat(recommendationV2.newlyCreated()).isTrue();
		assertThat(recommendationV2.recommendationId()).isNotEqualTo(recommendationId);
		assertThat(recommendationV2.dailyReadinessAssessmentId()).isEqualTo(readinessV2.assessmentId());
		assertThat(recommendationV2.dailyAthleteStateSnapshotVersion())
				.isEqualTo(snapshotV2.snapshotVersion());

		DailyTrainingRecommendationResult historical = getDailyTrainingRecommendationUseCase.execute(
				accountId, recommendationId);
		assertThat(historical.overallAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
		assertThat(historical.dailyAthleteStateSnapshotVersion()).isEqualTo(snapshot.snapshotVersion());

		DailyTrainingRecommendationComparisonResult comparison =
				compareDailyTrainingRecommendationsUseCase.execute(
						accountId, recommendationId, recommendationV2.recommendationId());
		assertThat(comparison.olderSnapshotVersion()).isEqualTo(snapshot.snapshotVersion());
		assertThat(comparison.newerSnapshotVersion()).isEqualTo(snapshotV2.snapshotVersion());
		assertThat(comparison.olderReadinessAssessmentId()).isEqualTo(readiness.assessmentId());
		assertThat(comparison.newerReadinessAssessmentId()).isEqualTo(readinessV2.assessmentId());
		assertThat(comparison.priorAction()).isEqualTo(TrainingRecommendationAction.MODIFY_SESSION);
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

	private void scheduleLowerBody(AccountId accountId, LocalDate scheduledDate) {
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Recommendation Block", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower Body", null, 1, DayOfWeek.FRIDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BACK_SQUAT, "Back Squat",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, 0);
		createWorkoutOccurrenceUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				scheduledDate,
				null,
				null);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Casey", "Nguyen", LocalDate.of(1995, 5, 20), Sex.MALE,
				Height.ofCentimeters(178), Weight.ofKilograms(78),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		return accountId;
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
