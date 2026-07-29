package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipType;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.SystemExerciseSubstitutionRelationships;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CanonicalExerciseClassificationIntegrationTests {

	private static final String BACK_SQUAT = "Back Squat";

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private AthleteContextPort athleteContextPort;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Autowired
	private CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;

	@Autowired
	private ListExerciseSubstitutionCandidatesUseCase listExerciseSubstitutionCandidatesUseCase;

	@Autowired
	private SubstituteWorkoutExerciseExecutionUseCase substituteWorkoutExerciseExecutionUseCase;

	@Autowired
	private ListWorkoutExerciseSubstitutionHistoryUseCase listWorkoutExerciseSubstitutionHistoryUseCase;

	@Autowired
	private ArchiveExerciseSubstitutionRelationshipUseCase archiveExerciseSubstitutionRelationshipUseCase;

	@Autowired
	private CreateExerciseSubstitutionRelationshipUseCase createExerciseSubstitutionRelationshipUseCase;

	@Autowired
	private ListAccessibleExerciseDefinitionsUseCase listAccessibleExerciseDefinitionsUseCase;

	@Test
	void catalogueMetadataFiltersCandidatesEquipmentAndRecordsRelationshipProvenance() {
		AccountId accountId = athlete();

		List<ExerciseSubstitutionCandidateResult> equipped = listExerciseSubstitutionCandidatesUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BACK_SQUAT,
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH));
		assertThat(equipped).extracting(ExerciseSubstitutionCandidateResult::targetExerciseDefinitionId)
				.contains(SystemExerciseDefinitions.GOBLET_SQUAT)
				.doesNotContain(SystemExerciseDefinitions.LEG_PRESS);
		assertThat(equipped.getFirst().compatibilityLevel()).isEqualTo(ExerciseSubstitutionCompatibility.HIGH);
		assertThat(equipped.getFirst().relationshipType())
				.isEqualTo(ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE);

		Session session = startSession(prescribe(accountId), LocalDate.of(2026, 4, 6));
		substituteWorkoutExerciseExecutionUseCase.execute(
				accountId,
				session.planId(),
				session.dayId(),
				session.occurrenceId(),
				session.executionId(),
				SystemExerciseDefinitions.GOBLET_SQUAT,
				ExerciseSubstitutionReason.FACILITY_CONSTRAINT,
				"Hotel gym",
				SystemExerciseSubstitutionRelationships.BACK_SQUAT_TO_GOBLET_SQUAT);

		WorkoutExerciseSubstitutionResult history = listWorkoutExerciseSubstitutionHistoryUseCase.execute(
				accountId, session.planId(), session.dayId(), session.occurrenceId(), session.executionId()).getFirst();
		assertThat(history.substitutionRelationshipId())
				.isEqualTo(SystemExerciseSubstitutionRelationships.BACK_SQUAT_TO_GOBLET_SQUAT);
		assertThat(history.relationshipTypeSnapshot())
				.isEqualTo(ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE);
		assertThat(history.compatibilitySnapshot()).isEqualTo(ExerciseSubstitutionCompatibility.HIGH);

		ExerciseDefinitionResult hotelSquat = createAthleteExerciseDefinitionUseCase.execute(
				accountId,
				"Hotel Dumbbell Squat",
				ExerciseDefinitionMetadataFixtures.hotelDumbbellSquat());

		ExerciseSubstitutionRelationshipResult ownedRelationship =
				createExerciseSubstitutionRelationshipUseCase.execute(
						accountId,
						SystemExerciseDefinitions.BACK_SQUAT,
						hotelSquat.id(),
						ExerciseSubstitutionRelationshipType.EQUIPMENT_ALTERNATIVE,
						ExerciseSubstitutionCompatibility.CONDITIONAL,
						"Limited hotel setup");

		assertThat(listExerciseSubstitutionCandidatesUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BACK_SQUAT,
				List.of(EquipmentType.DUMBBELL))
				.stream()
				.map(ExerciseSubstitutionCandidateResult::targetExerciseDefinitionId)
				.toList())
				.contains(hotelSquat.id(), SystemExerciseDefinitions.GOBLET_SQUAT);

		archiveExerciseSubstitutionRelationshipUseCase.execute(accountId, ownedRelationship.id());

		assertThat(listExerciseSubstitutionCandidatesUseCase.execute(
				accountId,
				SystemExerciseDefinitions.BACK_SQUAT,
				List.of(EquipmentType.DUMBBELL, EquipmentType.BENCH))
				.stream()
				.map(ExerciseSubstitutionCandidateResult::targetExerciseDefinitionId)
				.toList())
				.contains(SystemExerciseDefinitions.GOBLET_SQUAT)
				.doesNotContain(hotelSquat.id());

		ExerciseDefinitionPageResult squatCatalogue = listAccessibleExerciseDefinitionsUseCase.execute(
				accountId,
				null,
				null,
				null,
				null,
				com.devinolabs.uap.training.domain.MovementPattern.SQUAT,
				null,
				null,
				null,
				null,
				null,
				0,
				50);
		assertThat(squatCatalogue.definitions()).extracting(ExerciseDefinitionResult::id)
				.contains(SystemExerciseDefinitions.BACK_SQUAT, SystemExerciseDefinitions.GOBLET_SQUAT, hotelSquat.id());
		assertThat(squatCatalogue.definitions()).extracting(ExerciseDefinitionResult::metadata)
				.allSatisfy(metadata -> assertThat(metadata.primaryMovementPattern())
						.isEqualTo(com.devinolabs.uap.training.domain.MovementPattern.SQUAT));
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Robin",
				"Vega",
				LocalDate.of(1994, 3, 8),
				Sex.FEMALE,
				Height.ofCentimeters(168),
				Weight.ofKilograms(63),
				DominantHand.RIGHT,
				DominantFoot.LEFT);
		return accountId;
	}

	private Prescription prescribe(AccountId accountId) {
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Classification Plan", null,
				LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower", null, 1, DayOfWeek.MONDAY, null, null, null);
		WorkoutExerciseResult exercise = createWorkoutExerciseUseCase.execute(
				accountId,
				plan.id(),
				day.id(),
				SystemExerciseDefinitions.BACK_SQUAT,
				BACK_SQUAT,
				ExerciseCategory.STRENGTH,
				ExerciseType.BARBELL,
				5,
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
		return new Prescription(accountId, plan.id(), day.id(), exercise.id());
	}

	private Session startSession(Prescription prescription, LocalDate scheduledDate) {
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				prescription.accountId(), prescription.planId(), prescription.dayId(), scheduledDate, null, null);
		AthleteId athleteId = AthleteId.of(
				athleteContextPort.requireAthlete(prescription.accountId().value()).athleteId());
		return new Session(
				prescription,
				occurrence.occurrence().id(),
				occurrence.executions().getFirst().id(),
				athleteId);
	}

	private record Prescription(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutExerciseId exerciseId) {
	}

	private record Session(
			Prescription prescription,
			WorkoutOccurrenceId occurrenceId,
			WorkoutExerciseExecutionId executionId,
			AthleteId athleteId) {

		AccountId accountId() {
			return prescription.accountId();
		}

		TrainingPlanId planId() {
			return prescription.planId();
		}

		WorkoutDayId dayId() {
			return prescription.dayId();
		}
	}

}
