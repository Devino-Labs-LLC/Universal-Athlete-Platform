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

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitionModificationNotAllowedException;
import com.devinolabs.uap.training.domain.SystemExerciseDefinitions;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * Acceptance coverage for canonical exercise identity: performance history follows the exercise
 * definition, so the same movement prescribed in two different plans accumulates one set of records
 * instead of one per prescription.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CanonicalExerciseIdentityIntegrationTests {

	private static final String BACK_SQUAT = "Back Squat";

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
	private GetAthleteExercisePerformanceHistoryUseCase getAthleteExercisePerformanceHistoryUseCase;

	@Autowired
	private GetAthleteExercisePersonalRecordsUseCase getAthleteExercisePersonalRecordsUseCase;

	@Autowired
	private RebuildAthletePersonalRecordsUseCase rebuildAthletePersonalRecordsUseCase;

	@Autowired
	private CreateAthleteExerciseDefinitionUseCase createAthleteExerciseDefinitionUseCase;

	@Autowired
	private UpdateAthleteExerciseDefinitionUseCase updateAthleteExerciseDefinitionUseCase;

	@Autowired
	private ArchiveAthleteExerciseDefinitionUseCase archiveAthleteExerciseDefinitionUseCase;

	@Autowired
	private GetExerciseDefinitionUseCase getExerciseDefinitionUseCase;

	@Autowired
	private ListAccessibleExerciseDefinitionsUseCase listAccessibleExerciseDefinitionsUseCase;

	@Test
	void twoPlansPrescribingOneDefinitionShareASingleHistoryAndRecordSet() {
		AccountId accountId = athlete();
		ExercisePerformanceKey key = ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT);

		Prescription blockOne = prescribe(accountId, "Block One", "Lower One",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT);
		logSession(blockOne, LocalDate.of(2026, 4, 6), 5, "100");
		Prescription blockTwo = prescribe(accountId, "Block Two", "Lower Two",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT);
		logSession(blockTwo, LocalDate.of(2026, 5, 4), 3, "130");

		assertThat(blockOne.exerciseId()).isNotEqualTo(blockTwo.exerciseId());
		AthleteExercisePerformanceHistoryResult history = history(accountId, key);
		assertThat(history.totalElements()).isEqualTo(2);
		assertThat(history.entries()).extracting(ExerciseExecutionPerformanceResult::scheduledDate)
				.containsExactly(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 4, 6));
		assertThat(records(accountId, key).get("HEAVIEST_WEIGHT").normalizedValue())
				.isEqualByComparingTo("130");
		assertThat(records(accountId, key).get("MOST_REPETITIONS").normalizedValue())
				.isEqualByComparingTo("5");
	}

	@Test
	void rebuildAcrossPlansReplaysEveryExecutionAndStaysIdempotent() {
		AccountId accountId = athlete();
		ExercisePerformanceKey key = ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT);
		logSession(prescribe(accountId, "Block One", "Lower One",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT), LocalDate.of(2026, 4, 6), 5, "100");
		logSession(prescribe(accountId, "Block Two", "Lower Two",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT), LocalDate.of(2026, 5, 4), 3, "130");
		Map<String, String> before = fingerprint(records(accountId, key));

		PersonalRecordRebuildResult rebuild = rebuildAthletePersonalRecordsUseCase.execute(accountId, key);
		rebuildAthletePersonalRecordsUseCase.execute(accountId, key);

		assertThat(rebuild.replayedExecutionCount()).isEqualTo(2);
		assertThat(rebuild.personalRecordCount()).isEqualTo(before.size());
		assertThat(fingerprint(records(accountId, key))).isEqualTo(before);
	}

	@Test
	void renamingADefinitionKeepsItsIdentityAndTheNamesAlreadyLogged() {
		AccountId accountId = athlete();
		ExerciseDefinitionResult definition = createAthleteExerciseDefinitionUseCase.execute(
				accountId, "Bulgarian Split Squat");
		ExercisePerformanceKey key = ExercisePerformanceKey.of(definition.id());
		logSession(prescribe(accountId, "Block One", "Legs One", definition.id(), null),
				LocalDate.of(2026, 4, 6), 8, "40");

		ExerciseDefinitionResult renamed = updateAthleteExerciseDefinitionUseCase.execute(
				accountId, definition.id(), "Rear Foot Elevated Split Squat");
		Prescription afterRename = prescribe(accountId, "Block Two", "Legs Two", definition.id(), null);
		logSession(afterRename, LocalDate.of(2026, 5, 4), 8, "45");

		assertThat(renamed.id()).isEqualTo(definition.id());
		assertThat(renamed.canonicalName()).isEqualTo("Rear Foot Elevated Split Squat");
		assertThat(afterRename.exerciseName()).isEqualTo("Rear Foot Elevated Split Squat");
		assertThat(history(accountId, key).entries())
				.extracting(ExerciseExecutionPerformanceResult::exerciseName)
				.containsExactly("Rear Foot Elevated Split Squat", "Bulgarian Split Squat");
		assertThat(records(accountId, key).get("HEAVIEST_WEIGHT").normalizedValue())
				.isEqualByComparingTo("45");
	}

	@Test
	void aCustomDefinitionNamedLikeASystemOneKeepsItsOwnHistory() {
		AccountId accountId = athlete();
		ExerciseDefinitionResult custom = createAthleteExerciseDefinitionUseCase.execute(accountId, BACK_SQUAT);
		ExercisePerformanceKey systemKey = ExercisePerformanceKey.of(SystemExerciseDefinitions.BACK_SQUAT);
		ExercisePerformanceKey customKey = ExercisePerformanceKey.of(custom.id());

		logSession(prescribe(accountId, "Gym Block", "Gym Lower",
				SystemExerciseDefinitions.BACK_SQUAT, BACK_SQUAT), LocalDate.of(2026, 4, 6), 5, "140");
		logSession(prescribe(accountId, "Home Block", "Home Lower", custom.id(), "Home Back Squat"),
				LocalDate.of(2026, 4, 7), 5, "60");

		assertThat(custom.id()).isNotEqualTo(SystemExerciseDefinitions.BACK_SQUAT);
		assertThat(custom.scope()).isEqualTo(ExerciseDefinitionScope.ATHLETE_CUSTOM);
		assertThat(history(accountId, systemKey).totalElements()).isEqualTo(1);
		assertThat(history(accountId, customKey).totalElements()).isEqualTo(1);
		assertThat(records(accountId, systemKey).get("HEAVIEST_WEIGHT").normalizedValue())
				.isEqualByComparingTo("140");
		assertThat(records(accountId, customKey).get("HEAVIEST_WEIGHT").normalizedValue())
				.isEqualByComparingTo("60");
	}

	@Test
	void duplicateCustomNamesAreRejectedWhileTheDefinitionIsActive() {
		AccountId accountId = athlete();
		ExerciseDefinitionResult definition = createAthleteExerciseDefinitionUseCase.execute(accountId, "Sled Push");

		assertThatThrownBy(() -> createAthleteExerciseDefinitionUseCase.execute(accountId, "  sled   PUSH "))
				.isInstanceOf(DuplicateExerciseDefinitionException.class);
		assertThat(createAthleteExerciseDefinitionUseCase.execute(athlete(), "Sled Push").id())
				.isNotEqualTo(definition.id());
	}

	@Test
	void archivingRetiresADefinitionFromSelectionButKeepsItsHistoryReadable() {
		AccountId accountId = athlete();
		ExerciseDefinitionResult definition = createAthleteExerciseDefinitionUseCase.execute(accountId, "Sled Push");
		ExercisePerformanceKey key = ExercisePerformanceKey.of(definition.id());
		logSession(prescribe(accountId, "Block One", "Conditioning", definition.id(), null),
				LocalDate.of(2026, 4, 6), 6, "80");

		archiveAthleteExerciseDefinitionUseCase.execute(accountId, definition.id());

		assertThat(getExerciseDefinitionUseCase.execute(accountId, definition.id()).active()).isFalse();
		assertThat(getExerciseDefinitionUseCase.execute(accountId, definition.id()).archivedAt()).isNotNull();
		assertThat(listAccessibleExerciseDefinitionsUseCase
				.execute(accountId, "sled push", null, null, null).definitions()).isEmpty();
		assertThatThrownBy(() -> prescribe(accountId, "Block Two", "Conditioning Two", definition.id(), null))
				.isInstanceOf(ExerciseDefinitionArchivedException.class);
		assertThat(history(accountId, key).totalElements()).isEqualTo(1);
		assertThat(records(accountId, key).get("HEAVIEST_WEIGHT").normalizedValue()).isEqualByComparingTo("80");
		assertThat(createAthleteExerciseDefinitionUseCase.execute(accountId, "Sled Push").id())
				.isNotEqualTo(definition.id());
	}

	@Test
	void anotherAthletesCustomDefinitionIsNeitherReadableNorPrescribable() {
		AccountId owner = athlete();
		AccountId intruder = athlete();
		ExerciseDefinitionResult definition = createAthleteExerciseDefinitionUseCase.execute(owner, "Zercher Squat");

		assertThatThrownBy(() -> getExerciseDefinitionUseCase.execute(intruder, definition.id()))
				.isInstanceOf(ExerciseDefinitionNotAccessibleException.class);
		assertThatThrownBy(() -> prescribe(intruder, "Block One", "Lower One", definition.id(), "Zercher Squat"))
				.isInstanceOf(ExerciseDefinitionNotAccessibleException.class);
		assertThatThrownBy(() -> prescribe(owner, "Block One", "Lower One",
				ExerciseDefinitionId.generate(), "Ghost Lift"))
				.isInstanceOf(ExerciseDefinitionNotFoundException.class);
	}

	@Test
	void systemDefinitionsAreListedForEveryAthleteAndCannotBeModified() {
		AccountId accountId = athlete();

		ExerciseDefinitionPageResult systemDefinitions = listAccessibleExerciseDefinitionsUseCase.execute(
				accountId, null, ExerciseDefinitionScope.SYSTEM, null, null);

		assertThat(systemDefinitions.totalElements()).isEqualTo(SystemExerciseDefinitions.all().size());
		assertThat(systemDefinitions.definitions()).extracting(ExerciseDefinitionResult::canonicalName)
				.contains(BACK_SQUAT, "Front Squat", "Bench Press", "Romanian Deadlift");
		assertThat(systemDefinitions.definitions()).allSatisfy(definition -> {
			assertThat(definition.exercisePerformanceKey().value()).isEqualTo(definition.id().value());
			assertThat(definition.active()).isTrue();
		});
		assertThatThrownBy(() -> updateAthleteExerciseDefinitionUseCase.execute(
				accountId, SystemExerciseDefinitions.BACK_SQUAT, "Barbell Back Squat"))
				.isInstanceOf(SystemExerciseDefinitionModificationNotAllowedException.class);
		assertThatThrownBy(() -> archiveAthleteExerciseDefinitionUseCase.execute(
				accountId, SystemExerciseDefinitions.BACK_SQUAT))
				.isInstanceOf(SystemExerciseDefinitionModificationNotAllowedException.class);
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
				3, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);
		return new Prescription(accountId, plan.id(), day.id(), exercise.id(), exercise.exerciseName());
	}

	private void logSession(Prescription prescription, LocalDate scheduledDate, int reps, String weight) {
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				prescription.accountId(), prescription.planId(), prescription.dayId(), scheduledDate, null, null);
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		WorkoutExerciseExecutionId executionId = occurrence.executions().getFirst().id();
		List<WorkoutExerciseSetResult> sets = listWorkoutExerciseSetsUseCase.execute(
				prescription.accountId(), prescription.planId(), prescription.dayId(), occurrenceId, executionId);

		updateWorkoutExerciseSetUseCase.execute(
				prescription.accountId(), prescription.planId(), prescription.dayId(), occurrenceId, executionId,
				sets.getFirst().id(),
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
				prescription.accountId(), prescription.planId(), prescription.dayId(), occurrenceId, executionId,
				sets.getFirst().id());
		sets.stream().skip(1).forEach(set -> skipWorkoutExerciseSetUseCase.execute(
				prescription.accountId(), prescription.planId(), prescription.dayId(), occurrenceId, executionId,
				set.id()));
		completeWorkoutExerciseExecutionUseCase.execute(
				prescription.accountId(), prescription.planId(), prescription.dayId(), occurrenceId, executionId);
		completeWorkoutOccurrenceUseCase.execute(
				prescription.accountId(), prescription.planId(), prescription.dayId(), occurrenceId);
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

	private static Map<String, String> fingerprint(Map<String, PersonalRecordResult> records) {
		return records.entrySet().stream().collect(Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().normalizedValue().toPlainString()
						+ "@" + entry.getValue().achievedAt()
						+ "#" + entry.getValue().sourceSetId().value()));
	}

	private record Prescription(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			WorkoutExerciseId exerciseId,
			String exerciseName) {
	}

}
