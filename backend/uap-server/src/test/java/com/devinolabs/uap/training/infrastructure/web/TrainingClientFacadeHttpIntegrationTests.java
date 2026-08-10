package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.CreateAthleteExerciseDefinitionUseCase;
import com.devinolabs.uap.training.application.CreateDailyRecoveryCheckInUseCase;
import com.devinolabs.uap.training.application.CreateExerciseSubstitutionRelationshipUseCase;
import com.devinolabs.uap.training.application.CreateTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.CreateTrainingPlanUseCase;
import com.devinolabs.uap.training.application.CreateWorkoutDayUseCase;
import com.devinolabs.uap.training.application.CreateWorkoutExerciseUseCase;
import com.devinolabs.uap.training.application.CreateWorkoutOccurrenceUseCase;
import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotResult;
import com.devinolabs.uap.training.application.DailyReadinessAssessmentResult;
import com.devinolabs.uap.training.application.DailyTrainingRecommendationResult;
import com.devinolabs.uap.training.application.GenerateDailyAthleteStateSnapshotUseCase;
import com.devinolabs.uap.training.application.GenerateDailyReadinessAssessmentUseCase;
import com.devinolabs.uap.training.application.GenerateDailyTrainingRecommendationUseCase;
import com.devinolabs.uap.training.application.GenerateRecommendedWorkoutAdaptationProposalUseCase;
import com.devinolabs.uap.training.application.SetWorkoutOccurrenceTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.TrainingEnvironmentResult;
import com.devinolabs.uap.training.application.TrainingPlanResult;
import com.devinolabs.uap.training.application.WorkoutAdaptationProposalResult;
import com.devinolabs.uap.training.application.WorkoutDayResult;
import com.devinolabs.uap.training.application.WorkoutOccurrenceDetailResult;
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
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
		TestcontainersConfiguration.class,
		TrainingClientFacadeHttpIntegrationTests.MutableClockConfig.class
})
class TrainingClientFacadeHttpIntegrationTests {

	private static final LocalDate JULY_31 = LocalDate.of(2026, 7, 31);

	@Autowired
	private MockMvc mockMvc;

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

	@Test
	void clientFacadesRequireAuthAndReturnEmptyStatesOrPopulatedComposition() throws Exception {
		mockMvc.perform(get("/api/v1/training/client/today").param("date", "2026-07-31"))
				.andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/v1/training/client/bootstrap"))
				.andExpect(status().isUnauthorized());

		AccountId accountId = athlete();
		mockMvc.perform(get("/api/v1/training/client/today")
						.with(auth(accountId))
						.param("date", "2026-07-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.date").value("2026-07-31"))
				.andExpect(jsonPath("$.recovery.checkInPresent").value(false))
				.andExpect(jsonPath("$.athleteState.snapshotPresent").value(false))
				.andExpect(jsonPath("$.readiness.readinessPresent").value(false))
				.andExpect(jsonPath("$.recommendation.recommendationPresent").value(false))
				.andExpect(jsonPath("$.adaptation.activeProposalPresent").value(false))
				.andExpect(jsonPath("$.actions.canGenerateAthleteStateSnapshot.allowed").value(true));

		mockMvc.perform(get("/api/v1/training/client/bootstrap").with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.clientContractVersion").value("V1"))
				.andExpect(jsonPath("$.features.readinessEnabled").value(true))
				.andExpect(jsonPath("$.features.recommendationsEnabled").value(true))
				.andExpect(jsonPath("$.features.adaptationEnabled").value(true))
				.andExpect(jsonPath("$.limits.baselineWindows", hasSize(3)))
				.andExpect(jsonPath("$.limits.readinessAlgorithmVersion").value("READINESS_V1"))
				.andExpect(jsonPath("$.limits.recommendationAlgorithmVersion")
						.value("TRAINING_RECOMMENDATION_V1"))
				.andExpect(jsonPath("$.units.canonicalWeightUnit").value("KILOGRAM"))
				.andExpect(jsonPath("$.ratingScales.recoveryRatingMin").value(1))
				.andExpect(jsonPath("$.ratingScales.recoveryRatingMax").value(5));

		mockMvc.perform(get("/api/v1/training/client/today")
						.with(auth(accountId))
						.param("date", "2099-01-01"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAINING_CLIENT_DATE"));

		mockMvc.perform(get("/api/v1/training/client/recovery-overview")
						.with(auth(accountId))
						.param("date", "2026-07-31")
						.param("trendDays", "9"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_TRAINING_CLIENT_TREND_DAYS"));

		PopulatedDay day = populatedDay(accountId);

		mockMvc.perform(get("/api/v1/training/client/today")
						.with(auth(accountId))
						.param("date", "2026-07-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.recovery.checkInPresent").value(true))
				.andExpect(jsonPath("$.recovery.recoveryCheckInId").exists())
				.andExpect(jsonPath("$.athleteState.snapshotPresent").value(true))
				.andExpect(jsonPath("$.athleteState.dailyAthleteStateSnapshotId").value(day.snapshotId().toString()))
				.andExpect(jsonPath("$.readiness.readinessPresent").value(true))
				.andExpect(jsonPath("$.readiness.readinessScore").exists())
				.andExpect(jsonPath("$.readiness.readinessBand").value("LOW"))
				.andExpect(jsonPath("$.recommendation.recommendationPresent").value(true))
				.andExpect(jsonPath("$.recommendation.overallAction").value("MODIFY_SESSION"))
				.andExpect(jsonPath("$.training.primaryOccurrence.occurrenceId")
						.value(day.occurrenceId().toString()))
				.andExpect(jsonPath("$.training.primaryOccurrence.feasibilityStatus")
						.value("PARTIALLY_FEASIBLE"))
				.andExpect(jsonPath("$.adaptation.activeProposalPresent").value(true))
				.andExpect(jsonPath("$.adaptation.adaptationProposalId").value(day.proposalId().toString()))
				.andExpect(jsonPath("$.adaptation.origin").value("TRAINING_RECOMMENDATION"));

		mockMvc.perform(get("/api/v1/training/client/training-overview")
						.with(auth(accountId))
						.param("date", "2026-07-31"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.activePlans", hasSize(greaterThanOrEqualToOne())))
				.andExpect(jsonPath("$.upcomingOccurrences", hasSize(greaterThanOrEqualToOne())))
				.andExpect(jsonPath("$.activeEnvironments", hasSize(greaterThanOrEqualToOne())));

		mockMvc.perform(get("/api/v1/training/client/recovery-overview")
						.with(auth(accountId))
						.param("date", "2026-07-31")
						.param("trendDays", "7"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.checkInPresent").value(true))
				.andExpect(jsonPath("$.readinessPresent").value(true))
				.andExpect(jsonPath("$.recommendationPresent").value(true));

		mockMvc.perform(get("/api/v1/training/client/plans/" + day.planId()
						+ "/days/" + day.dayId()
						+ "/occurrences/" + day.occurrenceId()
						+ "/launch-context")
						.with(auth(accountId)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.occurrence.occurrenceId").value(day.occurrenceId().toString()))
				.andExpect(jsonPath("$.exercises", hasSize(greaterThanOrEqualToOne())))
				.andExpect(jsonPath("$.feasibility.feasibilityPresent").value(true))
				.andExpect(jsonPath("$.feasibility.status").value("PARTIALLY_FEASIBLE"))
				.andExpect(jsonPath("$.adaptation.activeProposalPresent").value(true))
				.andExpect(jsonPath("$.actions.canStart.allowed").value(true));

		AccountId foreign = athlete();
		mockMvc.perform(get("/api/v1/training/client/plans/" + day.planId()
						+ "/days/" + day.dayId()
						+ "/occurrences/" + day.occurrenceId()
						+ "/launch-context")
						.with(auth(foreign)))
				.andExpect(status().isNotFound());
	}

	private static org.hamcrest.Matcher<Integer> greaterThanOrEqualToOne() {
		return org.hamcrest.Matchers.greaterThanOrEqualTo(1);
	}

	private PopulatedDay populatedDay(AccountId accountId) {
		com.devinolabs.uap.training.domain.AccountId trainingAccountId = trainingAccountId(accountId);
		seedPriorCheckIns(accountId);
		ScheduledWorkout scheduled = scheduleMixedHomeGym(trainingAccountId, JULY_31);
		createDailyRecoveryCheckInUseCase.execute(
				trainingAccountId, JULY_31, 360, 3, 5, 5, 2, 3, 3,
				List.of(new BodyAreaDiscomfortObservation.Input("LOWER_BACK", "RIGHT", 2, null)), null);
		DailyAthleteStateSnapshotResult snapshot = generateDailyAthleteStateSnapshotUseCase.execute(
				trainingAccountId, JULY_31, 7);
		DailyReadinessAssessmentResult readiness = generateDailyReadinessAssessmentUseCase.execute(
				trainingAccountId, snapshot.snapshotId());
		DailyTrainingRecommendationResult recommendation = generateDailyTrainingRecommendationUseCase.execute(
				trainingAccountId, readiness.assessmentId());
		if (recommendation.overallAction() != TrainingRecommendationAction.MODIFY_SESSION) {
			throw new IllegalStateException("Expected MODIFY_SESSION but was " + recommendation.overallAction());
		}
		WorkoutAdaptationProposalResult proposal = generateRecommendedAdaptationUseCase.execute(
				trainingAccountId,
				recommendation.recommendationId(),
				scheduled.occurrenceId(),
				3,
				true,
				30);
		return new PopulatedDay(
				scheduled.planId().value(),
				scheduled.dayId().value(),
				scheduled.occurrenceId(),
				snapshot.snapshotId(),
				proposal.id().value());
	}

	private ScheduledWorkout scheduleMixedHomeGym(
			com.devinolabs.uap.training.domain.AccountId accountId,
			LocalDate date) {
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
				accountId, TrainingPlanType.STRENGTH, null, "Client HTTP " + UUID.randomUUID(), null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null, environment.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Upper", null, 1, DayOfWeek.FRIDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.BENCH_PRESS, "Bench Press",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("80"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, 0);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), SystemExerciseDefinitions.PLANK, "Plank",
				ExerciseCategory.STRENGTH, ExerciseType.BODYWEIGHT,
				3, null, null, null, null,
				60, null, null, null, null, null, null, 1);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), date, null, null);
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, environment.id());
		return new ScheduledWorkout(plan.id(), day.id(), occurrenceId.value());
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
				trainingAccountId(accountId),
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

	private static com.devinolabs.uap.training.domain.AccountId trainingAccountId(AccountId accountId) {
		return com.devinolabs.uap.training.domain.AccountId.of(accountId.value());
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Casey", "Ng", LocalDate.of(1991, 8, 19), Sex.FEMALE,
				Height.ofCentimeters(170), Weight.ofKilograms(64),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		return accountId;
	}

	private static RequestPostProcessor auth(AccountId accountId) {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				new AccountPrincipal(accountId),
				null,
				java.util.List.of());
		return authentication(authentication);
	}

	private record ScheduledWorkout(
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			UUID occurrenceId) {
	}

	private record PopulatedDay(
			UUID planId,
			UUID dayId,
			UUID occurrenceId,
			UUID snapshotId,
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
