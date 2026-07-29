package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.devinolabs.uap.ExerciseDefinitionMetadataFixtures;
import com.devinolabs.uap.training.application.UpdateAthleteExerciseDefinitionCommand;
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
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionReason;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseAlreadyUsesDefinitionException;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutExerciseNotSubstitutedException;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * Acceptance coverage for exercise substitution: what an athlete actually performs drives their
 * history and personal records, while the plan keeps prescribing what it always did.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CanonicalExerciseSubstitutionIntegrationTests {

	private static final String BACK_SQUAT = "Back Squat";
	private static final String GOBLET_SQUAT = "Goblet Squat";
	private static final String LEG_PRESS = "Leg Press";

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
	private GetWorkoutExerciseUseCase getWorkoutExerciseUseCase;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private UpdateWorkoutExerciseSetUseCase updateWorkoutExerciseSetUseCase;

	@Autowired
	private StartWorkoutExerciseSetUseCase startWorkoutExerciseSetUseCase;

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
	private RevertWorkoutExerciseExecutionSubstitutionUseCase revertWorkoutExerciseExecutionSubstitutionUseCase;

	@Autowired
	private ListWorkoutExerciseSubstitutionHistoryUseCase listWorkoutExerciseSubstitutionHistoryUseCase;

	@Autowired
	private GetAthleteExercisePerformanceHistoryUseCase getAthleteExercisePerformanceHistoryUseCase;

	@Autowired
	private GetAthleteExercisePersonalRecordsUseCase getAthleteExercisePersonalRecordsUseCase;

	@Autowired
	private CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;

	@Autowired
	private UpdateAthleteExerciseDefinitionUseCase updateAthleteExerciseDefinitionUseCase;

	@Autowired
	private ArchiveAthleteExerciseDefinitionUseCase archiveAthleteExerciseDefinitionUseCase;

	@Autowired
	private WorkoutExerciseExecutionRepository workoutExerciseExecutionRepository;

	@Test
	void aSubstitutionChainLeavesThePlanIntactAndFilesResultsUnderThePerformedMovement() {
		AccountId accountId = athlete();
		Prescription prescription = prescribe(accountId, "Block One", "Lower One",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT);
		Session session = startSession(prescription, LocalDate.of(2026, 4, 6));

		substitute(session, SystemExerciseDefinitions.GOBLET_SQUAT,
				ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE, "Rack taken");
		substitute(session, SystemExerciseDefinitions.LEG_PRESS,
				ExerciseSubstitutionReason.FATIGUE_MANAGEMENT, null);
		WorkoutExerciseExecutionResult reverted = revertWorkoutExerciseExecutionSubstitutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), session.occurrenceId(),
				session.executionId(), "Back on plan");
		WorkoutExerciseExecutionResult finalState = substitute(session, SystemExerciseDefinitions.GOBLET_SQUAT,
				ExerciseSubstitutionReason.INJURY, "Knee");

		assertThat(reverted.substituted()).isFalse();
		assertThat(reverted.performedExerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(finalState.prescribedExerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(finalState.prescribedExerciseName()).isEqualTo(BACK_SQUAT);
		assertThat(finalState.performedExerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.GOBLET_SQUAT);
		assertThat(finalState.performedExerciseName()).isEqualTo(GOBLET_SQUAT);
		assertThat(finalState.exerciseName()).isEqualTo(GOBLET_SQUAT);
		assertThat(finalState.exercisePerformanceKey())
				.isEqualTo(ExercisePerformanceKey.of(SystemExerciseDefinitions.GOBLET_SQUAT));
		assertThat(finalState.substitutionReason()).isEqualTo(ExerciseSubstitutionReason.INJURY);
		assertThat(finalState.substitutionNotes()).isEqualTo("Knee");
		assertThat(finalState.substitutedAt()).isNotNull();

		assertThat(substitutions(session))
				.extracting(
						WorkoutExerciseSubstitutionResult::fromExerciseName,
						WorkoutExerciseSubstitutionResult::toExerciseName,
						WorkoutExerciseSubstitutionResult::reason,
						WorkoutExerciseSubstitutionResult::reverted)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple(BACK_SQUAT, GOBLET_SQUAT,
								ExerciseSubstitutionReason.EQUIPMENT_UNAVAILABLE, false),
						org.assertj.core.groups.Tuple.tuple(GOBLET_SQUAT, LEG_PRESS,
								ExerciseSubstitutionReason.FATIGUE_MANAGEMENT, false),
						org.assertj.core.groups.Tuple.tuple(LEG_PRESS, BACK_SQUAT,
								ExerciseSubstitutionReason.REVERSION, true),
						org.assertj.core.groups.Tuple.tuple(BACK_SQUAT, GOBLET_SQUAT,
								ExerciseSubstitutionReason.INJURY, false));

		// The plan and the set prescriptions are untouched: the athlete swapped the movement, not
		// the work.
		assertThat(getWorkoutExerciseUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), prescription.exerciseId())
				.exerciseDefinitionId()).isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		List<WorkoutExerciseSetResult> sets = sets(session);
		assertThat(sets).hasSize(5);
		assertThat(sets).allSatisfy(set -> {
			assertThat(set.prescribedMinimumReps()).isEqualTo(5);
			assertThat(set.prescribedMaximumReps()).isEqualTo(5);
		});

		logAndComplete(session, 5, "60");

		ExercisePerformanceKey gobletKey = ExercisePerformanceKey.of(SystemExerciseDefinitions.GOBLET_SQUAT);
		ExercisePerformanceKey backSquatKey = ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(history(accountId, gobletKey).totalElements()).isEqualTo(1);
		assertThat(history(accountId, gobletKey).entries())
				.extracting(ExerciseExecutionPerformanceResult::exerciseName)
				.containsExactly(GOBLET_SQUAT);
		assertThat(records(accountId, gobletKey).get("HEAVIEST_WEIGHT").normalizedValue())
				.isEqualByComparingTo("60");
		assertThatThrownBy(() -> history(accountId, backSquatKey))
				.isInstanceOf(ExercisePerformanceKeyNotFoundException.class);
		assertThatThrownBy(() -> getAthleteExercisePersonalRecordsUseCase.execute(accountId, backSquatKey))
				.isInstanceOf(ExercisePerformanceKeyNotFoundException.class);
	}

	@Test
	void theNextOccurrenceIsPrescribedTheOriginalMovementAndStartsWithoutHistory() {
		AccountId accountId = athlete();
		Prescription prescription = prescribe(accountId, "Block One", "Lower One",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT);
		Session first = startSession(prescription, LocalDate.of(2026, 4, 6));
		substitute(first, SystemExerciseDefinitions.LEG_PRESS, ExerciseSubstitutionReason.INJURY, "Knee");
		logAndComplete(first, 8, "100");

		Session second = startSession(prescription, LocalDate.of(2026, 4, 13));

		assertThat(second.execution().substituted()).isFalse();
		assertThat(second.execution().prescribedExerciseDefinitionId())
				.isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(second.execution().performedExerciseDefinitionId())
				.isEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(second.execution().exercisePerformanceKey())
				.isEqualTo(ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT));
		assertThat(substitutions(second)).isEmpty();
		assertThat(substitutions(first)).hasSize(1);
	}

	@Test
	void renamingTheSubstituteAfterTheSessionKeepsTheNameThatWasLogged() {
		AccountId accountId = athlete();
		ExerciseDefinitionResult substitute = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Hack Squat Machine", ExerciseDefinitionMetadataFixtures.defaultCustom());
		Prescription prescription = prescribe(accountId, "Block One", "Lower One",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT);
		Session session = startSession(prescription, LocalDate.of(2026, 4, 6));
		substitute(session, substitute.id(), ExerciseSubstitutionReason.FACILITY_CONSTRAINT, null);
		logAndComplete(session, 10, "120");

		updateAthleteExerciseDefinitionUseCase.execute(
				accountId, substitute.id(), UpdateAthleteExerciseDefinitionCommand.renameOnly("Hack Squat"));

		ExercisePerformanceKey key = ExercisePerformanceKey.of(substitute.id());
		assertThat(history(accountId, key).entries())
				.extracting(ExerciseExecutionPerformanceResult::exerciseName)
				.containsExactly("Hack Squat Machine");
		assertThat(substitutions(session))
				.extracting(WorkoutExerciseSubstitutionResult::toExerciseName)
				.containsExactly("Hack Squat Machine");
		assertThat(records(accountId, key).get("HEAVIEST_WEIGHT").normalizedValue())
				.isEqualByComparingTo("120");
	}

	@Test
	void substitutionIsLockedAsSoonAsASetIsTouched() {
		AccountId accountId = athlete();
		Prescription prescription = prescribe(accountId, "Block One", "Lower One",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT);

		Session started = startSession(prescription, LocalDate.of(2026, 4, 6));
		startWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), started.occurrenceId(),
				started.executionId(), sets(started).getFirst().id());
		assertThatThrownBy(() -> substitute(started, SystemExerciseDefinitions.LEG_PRESS,
				ExerciseSubstitutionReason.INJURY, null))
				.isInstanceOf(WorkoutExerciseSubstitutionLockedException.class);

		Session skipped = startSession(prescription, LocalDate.of(2026, 4, 13));
		skipWorkoutExerciseSetUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), skipped.occurrenceId(),
				skipped.executionId(), sets(skipped).getFirst().id());
		assertThatThrownBy(() -> substitute(skipped, SystemExerciseDefinitions.LEG_PRESS,
				ExerciseSubstitutionReason.INJURY, null))
				.isInstanceOf(WorkoutExerciseSubstitutionLockedException.class);

		Session completed = startSession(prescription, LocalDate.of(2026, 4, 20));
		substitute(completed, SystemExerciseDefinitions.LEG_PRESS, ExerciseSubstitutionReason.INJURY, null);
		logAndComplete(completed, 8, "140");
		assertThatThrownBy(() -> substitute(completed, SystemExerciseDefinitions.GOBLET_SQUAT,
				ExerciseSubstitutionReason.INJURY, null))
				.isInstanceOf(InvalidWorkoutOccurrenceStatusException.class);
	}

	@Test
	void substitutionRejectsTheCurrentMovementAndRevertRequiresASubstitution() {
		AccountId accountId = athlete();
		Prescription prescription = prescribe(accountId, "Block One", "Lower One",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT);
		Session session = startSession(prescription, LocalDate.of(2026, 4, 6));

		assertThatThrownBy(() -> substitute(session, SystemExerciseDefinitions.BACK_SQUAT,
				ExerciseSubstitutionReason.ATHLETE_PREFERENCE, null))
				.isInstanceOf(WorkoutExerciseAlreadyUsesDefinitionException.class);
		assertThatThrownBy(() -> revertWorkoutExerciseExecutionSubstitutionUseCase.execute(
				accountId, prescription.planId(), prescription.dayId(), session.occurrenceId(),
				session.executionId(), null))
				.isInstanceOf(WorkoutExerciseNotSubstitutedException.class);
	}

	@Test
	void anArchivedMovementCannotBeSubstitutedInButAnArchivedPrescriptionStillGenerates() {
		AccountId accountId = athlete();
		ExerciseDefinitionResult retired = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Sled Push", ExerciseDefinitionMetadataFixtures.defaultCustom());
		ExerciseDefinitionResult prescribed = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Zercher Squat", ExerciseDefinitionMetadataFixtures.defaultCustom());
		Prescription prescription = prescribe(accountId, "Block One", "Lower One", prescribed.id(), null);
		archiveAthleteExerciseDefinitionUseCase.execute(accountId, retired.id());
		archiveAthleteExerciseDefinitionUseCase.execute(accountId, prescribed.id());

		Session session = startSession(prescription, LocalDate.of(2026, 4, 6));

		assertThat(session.execution().performedExerciseDefinitionId()).isEqualTo(prescribed.id());
		assertThatThrownBy(() -> substitute(session, retired.id(), ExerciseSubstitutionReason.INJURY, null))
				.isInstanceOf(ExerciseDefinitionArchivedException.class);
		AccountId intruder = athlete();
		ExerciseDefinitionResult theirs = createAthleteExerciseDefinitionUseCase.execute(
				intruder, "Belt Squat", ExerciseDefinitionMetadataFixtures.defaultCustom());
		assertThatThrownBy(() -> substitute(session, theirs.id(), ExerciseSubstitutionReason.INJURY, null))
				.isInstanceOf(ExerciseDefinitionNotAccessibleException.class);
		assertThatThrownBy(() -> substitute(session, ExerciseDefinitionId.generate(),
				ExerciseSubstitutionReason.INJURY, null))
				.isInstanceOf(ExerciseDefinitionNotFoundException.class);
	}

	@Test
	void aStaleSubstitutionLosesToTheOneThatLandedFirst() {
		AccountId accountId = athlete();
		Prescription prescription = prescribe(accountId, "Block One", "Lower One",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT);
		Session session = startSession(prescription, LocalDate.of(2026, 4, 6));
		WorkoutExerciseExecution stale = workoutExerciseExecutionRepository
				.findAllByWorkoutOccurrenceIdAndAthleteId(session.occurrenceId(), session.athleteId())
				.getFirst();
		assertThat(stale.version()).isZero();

		substitute(session, SystemExerciseDefinitions.LEG_PRESS, ExerciseSubstitutionReason.INJURY, null);

		stale.updateNotes("written from a stale copy", java.time.Clock.systemUTC());
		assertThatThrownBy(() -> workoutExerciseExecutionRepository.save(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	private AccountId athlete() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Sam",
				"Okafor",
				LocalDate.of(1997, 7, 21),
				Sex.MALE,
				Height.ofCentimeters(180),
				Weight.ofKilograms(80),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		return accountId;
	}

	private Prescription prescribe(
			AccountId accountId,
			String planName,
			String dayTitle,
			ExerciseDefinitionId definitionId,
			String exerciseName) {
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, planName, null,
				LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), dayTitle, null, 1, DayOfWeek.MONDAY, null, null, null);
		WorkoutExerciseResult exercise = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), definitionId,
				exerciseName, ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				5, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);
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
				occurrence.executions().getFirst(),
				athleteId);
	}

	private WorkoutExerciseExecutionResult substitute(
			Session session,
			ExerciseDefinitionId target,
			ExerciseSubstitutionReason reason,
			String notes) {
		return substitute(session, target, reason, notes, null);
	}

	private WorkoutExerciseExecutionResult substitute(
			Session session,
			ExerciseDefinitionId target,
			ExerciseSubstitutionReason reason,
			String notes,
			com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId relationshipId) {
		return substituteWorkoutExerciseExecutionUseCase.execute(
				session.prescription().accountId(),
				session.prescription().planId(),
				session.prescription().dayId(),
				session.occurrenceId(),
				session.executionId(),
				target,
				reason,
				notes,
				relationshipId);
	}

	private List<WorkoutExerciseSubstitutionResult> substitutions(Session session) {
		return listWorkoutExerciseSubstitutionHistoryUseCase.execute(
				session.prescription().accountId(),
				session.prescription().planId(),
				session.prescription().dayId(),
				session.occurrenceId(),
				session.executionId());
	}

	private List<WorkoutExerciseSetResult> sets(Session session) {
		return listWorkoutExerciseSetsUseCase.execute(
				session.prescription().accountId(),
				session.prescription().planId(),
				session.prescription().dayId(),
				session.occurrenceId(),
				session.executionId());
	}

	private void logAndComplete(Session session, int reps, String weight) {
		AccountId accountId = session.prescription().accountId();
		TrainingPlanId planId = session.prescription().planId();
		WorkoutDayId dayId = session.prescription().dayId();
		List<WorkoutExerciseSetResult> sets = sets(session);
		updateWorkoutExerciseSetUseCase.execute(
				accountId, planId, dayId, session.occurrenceId(), session.executionId(), sets.getFirst().id(),
				new UpdateWorkoutExerciseSetCommand(
						null, false,
						reps, true,
						new BigDecimal(weight), true,
						WeightUnit.KILOGRAM, true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false));
		completeWorkoutExerciseSetUseCase.execute(
				accountId, planId, dayId, session.occurrenceId(), session.executionId(), sets.getFirst().id());
		sets.stream().skip(1).forEach(set -> skipWorkoutExerciseSetUseCase.execute(
				accountId, planId, dayId, session.occurrenceId(), session.executionId(), set.id()));
		completeWorkoutExerciseExecutionUseCase.execute(
				accountId, planId, dayId, session.occurrenceId(), session.executionId());
		completeWorkoutOccurrenceUseCase.execute(accountId, planId, dayId, session.occurrenceId());
	}

	private AthleteExercisePerformanceHistoryResult history(AccountId accountId, ExercisePerformanceKey key) {
		return getAthleteExercisePerformanceHistoryUseCase.execute(accountId, key, null, null, null, null);
	}

	private Map<String, PersonalRecordResult> records(AccountId accountId, ExercisePerformanceKey key) {
		return getAthleteExercisePersonalRecordsUseCase.execute(accountId, key).stream()
				.collect(Collectors.toMap(
						record -> record.recordQualifier() == null
								? record.recordType().name()
								: record.recordType().name() + "|" + record.recordQualifier(),
						Function.identity()));
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
			WorkoutExerciseExecutionResult execution,
			AthleteId athleteId) {

		WorkoutExerciseExecutionId executionId() {
			return execution.id();
		}
	}

}
