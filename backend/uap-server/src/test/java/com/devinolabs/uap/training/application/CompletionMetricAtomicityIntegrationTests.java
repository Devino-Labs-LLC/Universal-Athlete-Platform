package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import com.devinolabs.uap.ExerciseDefinitionFixtures;
import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecord;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.PersonalRecordType;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

/**
 * Metric processing runs inside the completion transaction, so a personal record that cannot be
 * written must leave the execution and its occurrence exactly as they were.
 */
@SpringBootTest
@Import({ TestcontainersConfiguration.class, CompletionMetricAtomicityIntegrationTests.FailingRecords.class })
class CompletionMetricAtomicityIntegrationTests {

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateTrainingPlanUseCase createTrainingPlanUseCase;

	@Autowired
	private CreateWorkoutDayUseCase createWorkoutDayUseCase;

	@Autowired
	private CreateWorkoutExerciseUseCase createWorkoutExerciseUseCase;

	@Autowired
	private ExerciseDefinitionFixtures exerciseDefinitions;

	@Autowired
	private CreateWorkoutOccurrenceUseCase createWorkoutOccurrenceUseCase;

	@Autowired
	private ListWorkoutExerciseSetsUseCase listWorkoutExerciseSetsUseCase;

	@Autowired
	private UpdateWorkoutExerciseSetUseCase updateWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseSetUseCase completeWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;

	@Autowired
	private GetWorkoutOccurrenceUseCase getWorkoutOccurrenceUseCase;

	@Test
	void aFailedPersonalRecordWriteRollsTheCompletionBack() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Sam",
				"Okafor",
				LocalDate.of(1994, 7, 19),
				Sex.MALE,
				Height.ofCentimeters(180),
				Weight.ofKilograms(80),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Atomicity", null,
				LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower", null, 1, DayOfWeek.MONDAY, null, null, null);
		createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), exerciseDefinitions.idFor(accountId, "Back Squat"),
				"Back Squat", ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				1, 5, 5, new BigDecimal("100"), WeightUnit.KILOGRAM,
				null, null, null, null, null, null, null, null);
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), LocalDate.of(2026, 5, 4), null, null);
		WorkoutOccurrenceId occurrenceId = occurrence.occurrence().id();
		WorkoutExerciseExecutionId executionId = occurrence.executions().getFirst().id();

		WorkoutExerciseSetResult set = listWorkoutExerciseSetsUseCase
				.execute(accountId, plan.id(), day.id(), occurrenceId, executionId)
				.getFirst();
		updateWorkoutExerciseSetUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, executionId, set.id(),
				new UpdateWorkoutExerciseSetCommand(
						null, false,
						5, true,
						new BigDecimal("100"), true,
						WeightUnit.KILOGRAM, true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false));
		completeWorkoutExerciseSetUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, executionId, set.id());

		assertThatThrownBy(() -> completeWorkoutExerciseExecutionUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId, executionId))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("personal record storage is unavailable");

		WorkoutOccurrenceDetailResult reloaded = getWorkoutOccurrenceUseCase.execute(
				accountId, plan.id(), day.id(), occurrenceId);
		assertThat(reloaded.occurrence().status()).isEqualTo(WorkoutOccurrenceStatus.IN_PROGRESS);
		assertThat(reloaded.executions().getFirst().status())
				.isEqualTo(WorkoutExerciseExecutionStatus.IN_PROGRESS);
		assertThat(reloaded.executions().getFirst().completedAt()).isNull();
		assertThat(reloaded.executions().getFirst().actualSets()).isNull();
		assertThat(listWorkoutExerciseSetsUseCase
				.execute(accountId, plan.id(), day.id(), occurrenceId, executionId)
				.getFirst()
				.status())
				.isEqualTo(WorkoutExerciseSetStatus.COMPLETED);
	}

	@TestConfiguration
	static class FailingRecords {

		@Bean
		@Primary
		AthleteExercisePersonalRecordRepository failingPersonalRecordRepository() {
			return new AthleteExercisePersonalRecordRepository() {

				@Override
				public AthleteExercisePersonalRecord save(AthleteExercisePersonalRecord record) {
					throw new IllegalStateException("personal record storage is unavailable");
				}

				@Override
				public List<AthleteExercisePersonalRecord> findAllByAthleteIdAndExercisePerformanceKey(
						AthleteId athleteId,
						ExercisePerformanceKey exercisePerformanceKey) {
					return List.of();
				}

				@Override
				public List<AthleteExercisePersonalRecord> findAllByAthleteId(
						AthleteId athleteId,
						ExercisePerformanceKey exercisePerformanceKey,
						PersonalRecordType recordType) {
					return List.of();
				}

				@Override
				public List<AthleteExercisePersonalRecord> findRecentByAthleteId(
						AthleteId athleteId,
						Instant achievedFrom,
						int limit) {
					return List.of();
				}

				@Override
				public void deleteAllByAthleteId(
						AthleteId athleteId,
						ExercisePerformanceKey exercisePerformanceKey) {
				}

			};
		}

	}

}
