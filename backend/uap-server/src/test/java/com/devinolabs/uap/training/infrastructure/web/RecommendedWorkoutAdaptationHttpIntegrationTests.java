package com.devinolabs.uap.training.infrastructure.web;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
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
import com.devinolabs.uap.training.application.GenerateWorkoutAdaptationProposalUseCase;
import com.devinolabs.uap.training.application.SetWorkoutOccurrenceTrainingEnvironmentUseCase;
import com.devinolabs.uap.training.application.TrainingEnvironmentResult;
import com.devinolabs.uap.training.application.TrainingPlanResult;
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
import com.jayway.jsonpath.JsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@Import({
		TestcontainersConfiguration.class,
		RecommendedWorkoutAdaptationHttpIntegrationTests.MutableClockConfig.class
})
class RecommendedWorkoutAdaptationHttpIntegrationTests {

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
	private GenerateWorkoutAdaptationProposalUseCase generateManualAdaptationUseCase;

	@Test
	void recommendedAdaptationEndpointRequiresAuthCsrfAndReturnsProvenance() throws Exception {
		Session session = sessionWithModifySession();
		String url = "/api/v1/training/recommendations/%s/occurrences/%s/adaptation-proposals"
				.formatted(session.recommendationId(), session.occurrenceId());

		mockMvc.perform(post(url).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(post(url)
						.with(auth(session.accountId()))
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isForbidden());

		MvcResult created = mockMvc.perform(post(url)
						.with(auth(session.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "suggestionLimit": 3,
								  "includeAlternatives": true,
								  "expirationMinutes": 30
								}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.origin").value("TRAINING_RECOMMENDATION"))
				.andExpect(jsonPath("$.recommendationProvenance.recommendationId")
						.value(session.recommendationId().toString()))
				.andExpect(jsonPath("$.recommendationProvenance.readinessAssessmentId")
						.value(session.readinessAssessmentId().toString()))
				.andExpect(jsonPath("$.recommendationProvenance.stateSnapshotId")
						.value(session.snapshotId().toString()))
				.andExpect(jsonPath("$.recommendationProvenance.overallAction").value("MODIFY_SESSION"))
				.andExpect(jsonPath("$.recommendationAdjustments.length()", greaterThan(0)))
				.andExpect(jsonPath("$.items", hasSize(1)))
				.andReturn();

		String proposalId = JsonPath.read(created.getResponse().getContentAsString(), "$.id");

		mockMvc.perform(post(url)
						.with(auth(session.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("ACTIVE_WORKOUT_ADAPTATION_PROPOSAL_EXISTS"));

		mockMvc.perform(post("/api/v1/training/adaptation-proposals/" + proposalId + "/cancel")
						.with(auth(session.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.origin").value("TRAINING_RECOMMENDATION"));
	}

	@Test
	void recommendedAdaptationRejectsOccurrenceMismatchAndLockedOccurrence() throws Exception {
		Session session = sessionWithModifySession();
		WorkoutOccurrenceDetailResult foreign = createWorkoutOccurrenceUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(session.accountId().value()),
				session.planId(),
				session.dayId(),
				LocalDate.of(2026, 8, 7),
				null,
				null);

		mockMvc.perform(post("/api/v1/training/recommendations/%s/occurrences/%s/adaptation-proposals"
						.formatted(session.recommendationId(), foreign.occurrence().id().value()))
						.with(auth(session.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("TRAINING_RECOMMENDATION_OCCURRENCE_MISMATCH"));

		mockMvc.perform(post("/api/v1/training/plans/%s/days/%s/occurrences/%s/exercises/%s/sets/%s/start"
						.formatted(
								session.planId().value(),
								session.dayId().value(),
								session.occurrenceId(),
								session.executionId(),
								session.setId()))
						.with(auth(session.accountId()))
						.with(csrf()))
				.andExpect(status().isOk());

		mockMvc.perform(post("/api/v1/training/recommendations/%s/occurrences/%s/adaptation-proposals"
						.formatted(session.recommendationId(), session.occurrenceId()))
						.with(auth(session.accountId()))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("RECOMMENDED_ADAPTATION_OCCURRENCE_LOCKED"));
	}

	@Test
	void manualProposalKeepsNullRecommendationProvenance() throws Exception {
		Session session = sessionWithModifySession();
		var manual = generateManualAdaptationUseCase.execute(
				com.devinolabs.uap.training.domain.AccountId.of(session.accountId().value()),
				session.planId(),
				session.dayId(),
				com.devinolabs.uap.training.domain.WorkoutOccurrenceId.of(session.occurrenceId()),
				3,
				false,
				30);
		mockMvc.perform(post("/api/v1/training/adaptation-proposals/" + manual.id().value() + "/cancel")
						.with(auth(session.accountId()))
						.with(csrf()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.origin").value("MANUAL"))
				.andExpect(jsonPath("$.recommendationProvenance").value(nullValue()))
				.andExpect(jsonPath("$.recommendationAdjustments", hasSize(0)));
	}

	private Session sessionWithModifySession() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Ava", "Brooks", LocalDate.of(1996, 1, 8), Sex.FEMALE,
				Height.ofCentimeters(170), Weight.ofKilograms(64),
				DominantHand.RIGHT, DominantFoot.RIGHT);
		com.devinolabs.uap.training.domain.AccountId trainingAccountId =
				com.devinolabs.uap.training.domain.AccountId.of(accountId.value());

		createDailyRecoveryCheckInUseCase.execute(trainingAccountId, LocalDate.of(2026, 7, 24), 420, 3, 3, 2, 2, 4, 4, null, null);
		createDailyRecoveryCheckInUseCase.execute(trainingAccountId, LocalDate.of(2026, 7, 25), 450, 4, 2, 2, 3, 4, 4, null, null);
		createDailyRecoveryCheckInUseCase.execute(trainingAccountId, LocalDate.of(2026, 7, 26), 390, 3, 4, 3, 3, 3, 3, null, null);
		createDailyRecoveryCheckInUseCase.execute(trainingAccountId, LocalDate.of(2026, 7, 27), 435, 4, 3, 3, 2, 4, 4, null, null);
		createDailyRecoveryCheckInUseCase.execute(trainingAccountId, LocalDate.of(2026, 7, 28), 420, 3, 3, 2, 3, 4, 3, null, null);
		createDailyRecoveryCheckInUseCase.execute(trainingAccountId, LocalDate.of(2026, 7, 29), 405, 3, 4, 3, 4, 3, 3, null, null);
		createDailyRecoveryCheckInUseCase.execute(trainingAccountId, LocalDate.of(2026, 7, 30), 450, 4, 3, 2, 2, 4, 4, null, null);

		TrainingEnvironmentResult homeGym = createTrainingEnvironmentUseCase.execute(
				trainingAccountId,
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
		ExerciseDefinitionId alternative = createAthleteExerciseDefinitionUseCase.execute(
				trainingAccountId,
				"DB Bench Alt",
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
				trainingAccountId,
				SystemExerciseDefinitions.BENCH_PRESS,
				alternative,
				ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
				ExerciseSubstitutionCompatibility.HIGH,
				"Home option");

		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				trainingAccountId, TrainingPlanType.STRENGTH, null, "HTTP Rec Adapt", null,
				LocalDate.of(2026, 6, 1), LocalDate.of(2026, 12, 31), null, null, homeGym.id().value());
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				trainingAccountId, plan.id(), "Upper", null, 1, DayOfWeek.FRIDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				trainingAccountId, plan.id(), day.id(), SystemExerciseDefinitions.BENCH_PRESS, "Bench Press",
				ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				3, 5, 5, new BigDecimal("80"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				trainingAccountId, plan.id(), day.id(), JULY_31, null, null);
		setWorkoutOccurrenceTrainingEnvironmentUseCase.execute(
				trainingAccountId, plan.id(), day.id(), occurrence.occurrence().id(), homeGym.id());

		createDailyRecoveryCheckInUseCase.execute(
				trainingAccountId,
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
				trainingAccountId, JULY_31, 7);
		DailyReadinessAssessmentResult readiness = generateDailyReadinessAssessmentUseCase.execute(
				trainingAccountId, snapshot.snapshotId());
		DailyTrainingRecommendationResult recommendation = generateDailyTrainingRecommendationUseCase.execute(
				trainingAccountId, readiness.assessmentId());
		if (recommendation.overallAction() != TrainingRecommendationAction.MODIFY_SESSION) {
			throw new IllegalStateException("Expected MODIFY_SESSION for HTTP fixture but was "
					+ recommendation.overallAction()
					+ " band=" + recommendation.readinessBand()
					+ " completeness=" + snapshot.completeness()
					+ " metrics=" + snapshot.recoveryMetrics().size()
					+ " checkInId=" + snapshot.recoveryCheckInId()
					+ " scheduled=" + recommendation.scheduledOccurrenceCount()
					+ " modifiable=" + recommendation.modifiableScheduledOccurrenceCount()
					+ " limiting=" + recommendation.limitingDimensions());
		}

		UUID executionId = occurrence.executions().getFirst().id().value();
		UUID setId;
		try {
			MvcResult sets = mockMvc.perform(
							org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
											"/api/v1/training/plans/%s/days/%s/occurrences/%s/exercises/%s/sets"
													.formatted(
															plan.id().value(),
															day.id().value(),
															occurrence.occurrence().id().value(),
															executionId))
									.with(auth(accountId)))
					.andExpect(status().isOk())
					.andReturn();
			setId = UUID.fromString(JsonPath.read(sets.getResponse().getContentAsString(), "$[0].id"));
		}
		catch (Exception ex) {
			throw new IllegalStateException(ex);
		}

		return new Session(
				accountId,
				plan.id(),
				day.id(),
				occurrence.occurrence().id().value(),
				executionId,
				setId,
				recommendation.recommendationId(),
				readiness.assessmentId(),
				snapshot.snapshotId());
	}

	private static RequestPostProcessor auth(AccountId accountId) {
		Authentication authentication = new UsernamePasswordAuthenticationToken(
				new AccountPrincipal(accountId),
				"n/a",
				List.of());
		return authentication(authentication);
	}

	private record Session(
			AccountId accountId,
			com.devinolabs.uap.training.domain.TrainingPlanId planId,
			com.devinolabs.uap.training.domain.WorkoutDayId dayId,
			UUID occurrenceId,
			UUID executionId,
			UUID setId,
			UUID recommendationId,
			UUID readinessAssessmentId,
			UUID snapshotId) {
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
