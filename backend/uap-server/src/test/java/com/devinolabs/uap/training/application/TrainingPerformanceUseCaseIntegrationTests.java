package com.devinolabs.uap.training.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import com.devinolabs.uap.ExerciseDefinitionFixtures;
import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.application.AthleteRepository;
import com.devinolabs.uap.athlete.application.CreateAthleteProfileUseCase;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecordHistory;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.ExerciseType;
import com.devinolabs.uap.training.domain.PersonalRecordType;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanType;
import com.devinolabs.uap.training.domain.WeightUnit;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

/**
 * Acceptance coverage for the back squat scenario: four sessions logged in mixed units must settle
 * on one deterministic set of personal records that survives recomputation and a full rebuild.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TrainingPerformanceUseCaseIntegrationTests {

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
	private SkipWorkoutExerciseSetUseCase skipWorkoutExerciseSetUseCase;

	@Autowired
	private CompleteWorkoutExerciseExecutionUseCase completeWorkoutExerciseExecutionUseCase;

	@Autowired
	private SkipWorkoutExerciseExecutionUseCase skipWorkoutExerciseExecutionUseCase;

	@Autowired
	private CompleteWorkoutOccurrenceUseCase completeWorkoutOccurrenceUseCase;

	@Autowired
	private SkipWorkoutOccurrenceUseCase skipWorkoutOccurrenceUseCase;

	@Autowired
	private GetWorkoutOccurrencePerformanceSummaryUseCase getWorkoutOccurrencePerformanceSummaryUseCase;

	@Autowired
	private GetAthleteExercisePerformanceHistoryUseCase getAthleteExercisePerformanceHistoryUseCase;

	@Autowired
	private GetAthleteExercisePersonalRecordsUseCase getAthleteExercisePersonalRecordsUseCase;

	@Autowired
	private ListAthletePersonalRecordsUseCase listAthletePersonalRecordsUseCase;

	@Autowired
	private GetRecentAthletePersonalRecordsUseCase getRecentAthletePersonalRecordsUseCase;

	@Autowired
	private RecomputeWorkoutExerciseExecutionMetricsUseCase recomputeWorkoutExerciseExecutionMetricsUseCase;

	@Autowired
	private RebuildAthletePersonalRecordsUseCase rebuildAthletePersonalRecordsUseCase;

	@Autowired
	private AthleteExercisePersonalRecordHistoryRepository personalRecordHistoryRepository;

	@Autowired
	private AthleteRepository athleteRepository;

	@Test
	void performanceKeyComesFromTheDefinitionRatherThanThePrescription() {
		Fixture fixture = fixture();
		Session session = session(fixture, LocalDate.of(2026, 4, 6));

		assertThat(session.exercisePerformanceKey()).isEqualTo(ExercisePerformanceKey.of(fixture.definitionId()));
		assertThat(session.exercisePerformanceKey().value()).isNotEqualTo(fixture.exerciseId());
		assertThat(session(fixture, LocalDate.of(2026, 4, 13)).exercisePerformanceKey())
				.isEqualTo(session.exercisePerformanceKey());
	}

	@Test
	void firstHeavySessionEstablishesEveryRecordDimension() {
		Fixture fixture = fixture();
		Session session = session(fixture, LocalDate.of(2026, 4, 6));
		logPoundSession(session);

		Map<String, PersonalRecordResult> records = recordsByKey(fixture);

		assertThat(records.get("HEAVIEST_WEIGHT").normalizedValue()).isEqualByComparingTo("102.0583");
		assertThat(records.get("HEAVIEST_WEIGHT").measuredValue()).isEqualByComparingTo("225");
		assertThat(records.get("HEAVIEST_WEIGHT").measuredUnit()).isEqualTo("POUND");
		assertThat(records.get("HEAVIEST_WEIGHT").estimated()).isFalse();
		assertThat(records.get("HEAVIEST_WEIGHT").exerciseName()).isEqualTo(BACK_SQUAT);
		assertThat(records.get("MOST_REPETITIONS").normalizedValue()).isEqualByComparingTo("6");
		assertThat(records.get("MOST_REPETITIONS_AT_WEIGHT|102.0583").normalizedValue()).isEqualByComparingTo("5");
		assertThat(records.get("MOST_REPETITIONS_AT_WEIGHT|92.9864").normalizedValue()).isEqualByComparingTo("6");
		assertThat(records.get("HIGHEST_ESTIMATED_ONE_REP_MAX").normalizedValue()).isEqualByComparingTo("119.0680");
		assertThat(records.get("HIGHEST_ESTIMATED_ONE_REP_MAX").measuredValue()).isEqualByComparingTo("262.5000");
		assertThat(records.get("HIGHEST_ESTIMATED_ONE_REP_MAX").estimated()).isTrue();
		assertThat(records.get("HIGHEST_SET_VOLUME").normalizedValue()).isEqualByComparingTo("557.9184");
	}

	@Test
	void occurrenceSummaryRollsUpTheWholeSession() {
		Fixture fixture = fixture();
		Session session = session(fixture, LocalDate.of(2026, 4, 6));
		logPoundSession(session);

		WorkoutOccurrencePerformanceResult summary = getWorkoutOccurrencePerformanceSummaryUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), session.occurrenceId());

		assertThat(summary.totals().completedExerciseCount()).isEqualTo(1);
		assertThat(summary.totals().completedSetCount()).isEqualTo(5);
		assertThat(summary.totals().totalRepetitions()).isEqualTo(25);
		assertThat(summary.totals().totalVolumeKilogramRepetitions()).isEqualByComparingTo("2497.0261");
		assertThat(summary.exercises()).singleElement().satisfies(exercise -> {
			assertThat(exercise.exerciseName()).isEqualTo(BACK_SQUAT);
			assertThat(exercise.status()).isEqualTo(WorkoutExerciseExecutionStatus.COMPLETED);
			assertThat(exercise.metrics().mostRepetitionsInSet()).isEqualTo(6);
			assertThat(exercise.metrics().heaviestWeight().normalizedValue()).isEqualByComparingTo("102.0583");
		});
	}

	@Test
	void aHeavierKilogramSessionTakesOverTheWeightRecords() {
		Fixture fixture = fixture();
		logPoundSession(session(fixture, LocalDate.of(2026, 4, 6)));
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 13)), 3, "120", WeightUnit.KILOGRAM);

		Map<String, PersonalRecordResult> records = recordsByKey(fixture);

		assertThat(records.get("HEAVIEST_WEIGHT").normalizedValue()).isEqualByComparingTo("120.0000");
		assertThat(records.get("HEAVIEST_WEIGHT").measuredUnit()).isEqualTo("KILOGRAM");
		assertThat(records.get("HIGHEST_ESTIMATED_ONE_REP_MAX").normalizedValue()).isEqualByComparingTo("132.0000");
		assertThat(records.get("MOST_REPETITIONS").normalizedValue()).isEqualByComparingTo("6");
	}

	@Test
	void anEquivalentPoundSessionTiesAndLeavesTheEarlierRecordInPlace() {
		Fixture fixture = fixture();
		logPoundSession(session(fixture, LocalDate.of(2026, 4, 6)));
		Session kilograms = session(fixture, LocalDate.of(2026, 4, 13));
		logSingleSet(kilograms, 3, "120", WeightUnit.KILOGRAM);
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 20)), 3, "264.5547", WeightUnit.POUND);

		PersonalRecordResult heaviest = recordsByKey(fixture).get("HEAVIEST_WEIGHT");

		assertThat(heaviest.normalizedValue()).isEqualByComparingTo("120.0000");
		assertThat(heaviest.measuredUnit()).isEqualTo("KILOGRAM");
		assertThat(heaviest.sourceExecutionId()).isEqualTo(kilograms.executionId());
		assertThat(heaviest.scheduledDate()).isEqualTo(LocalDate.of(2026, 4, 13));
	}

	@Test
	void historyKeepsEveryBeatenRecordAndPointsAtItsSuccessor() {
		Fixture fixture = fixture();
		logPoundSession(session(fixture, LocalDate.of(2026, 4, 6)));
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 13)), 3, "120", WeightUnit.KILOGRAM);
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 20)), 3, "264.5547", WeightUnit.POUND);

		List<AthleteExercisePersonalRecordHistory> history = heaviestWeightHistory(fixture);

		assertThat(history).hasSize(2);
		assertThat(history.get(0).measurement().normalizedValue()).isEqualByComparingTo("102.0583");
		assertThat(history.get(0).isCurrent()).isFalse();
		assertThat(history.get(0).supersededByHistoryId()).isEqualTo(history.get(1).id());
		assertThat(history.get(1).measurement().normalizedValue()).isEqualByComparingTo("120.0000");
		assertThat(history.get(1).isCurrent()).isTrue();
	}

	@Test
	void recomputingACompletedExecutionChangesNothing() {
		Fixture fixture = fixture();
		logPoundSession(session(fixture, LocalDate.of(2026, 4, 6)));
		Session kilograms = session(fixture, LocalDate.of(2026, 4, 13));
		logSingleSet(kilograms, 3, "120", WeightUnit.KILOGRAM);
		Map<String, PersonalRecordResult> before = recordsByKey(fixture);

		ExerciseExecutionPerformanceResult recomputed = recomputeWorkoutExerciseExecutionMetricsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(),
				kilograms.occurrenceId(), kilograms.executionId());

		assertThat(recomputed.metrics().heaviestWeight().normalizedValue()).isEqualByComparingTo("120.0000");
		assertThat(recordsByKey(fixture)).usingRecursiveComparison().isEqualTo(before);
		assertThat(heaviestWeightHistory(fixture)).hasSize(2);
	}

	@Test
	void rebuildReproducesTheSameProjectionAndHistory() {
		Fixture fixture = fixture();
		logPoundSession(session(fixture, LocalDate.of(2026, 4, 6)));
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 13)), 3, "120", WeightUnit.KILOGRAM);
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 20)), 3, "264.5547", WeightUnit.POUND);
		Map<String, PersonalRecordResult> before = recordsByKey(fixture);
		List<String> historyBefore = historyFingerprint(fixture);

		PersonalRecordRebuildResult first = rebuildAthletePersonalRecordsUseCase.execute(
				fixture.accountId(), ExercisePerformanceKey.of(fixture.definitionId()));
		Map<String, PersonalRecordResult> afterFirst = recordsByKey(fixture);
		rebuildAthletePersonalRecordsUseCase.execute(
				fixture.accountId(), ExercisePerformanceKey.of(fixture.definitionId()));

		assertThat(first.replayedExecutionCount()).isEqualTo(3);
		assertThat(first.personalRecordCount()).isEqualTo(before.size());
		assertThat(afterFirst.keySet()).isEqualTo(before.keySet());
		assertThat(comparableValues(afterFirst)).isEqualTo(comparableValues(before));
		assertThat(comparableValues(recordsByKey(fixture))).isEqualTo(comparableValues(before));
		assertThat(historyFingerprint(fixture)).isEqualTo(historyBefore);
	}

	@Test
	void rebuildLeavesTheUnderlyingWorkoutRowsUntouched() {
		Fixture fixture = fixture();
		Session session = session(fixture, LocalDate.of(2026, 4, 6));
		logPoundSession(session);
		List<WorkoutExerciseSetResult> before = listSets(fixture, session);

		rebuildAthletePersonalRecordsUseCase.execute(fixture.accountId(), null);

		assertThat(listSets(fixture, session)).usingRecursiveComparison().isEqualTo(before);
	}

	@Test
	void skippedAndCancelledWorkNeverReachesTheRecords() {
		Fixture fixture = fixture();
		Session skippedExecution = session(fixture, LocalDate.of(2026, 4, 6));
		skipWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(),
				skippedExecution.occurrenceId(), skippedExecution.executionId());

		Session skippedOccurrence = session(fixture, LocalDate.of(2026, 4, 13));
		skipWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), skippedOccurrence.occurrenceId());

		assertThatThrownBy(() -> getAthleteExercisePersonalRecordsUseCase.execute(
				fixture.accountId(), ExercisePerformanceKey.of(UUID.randomUUID())))
				.isInstanceOf(ExercisePerformanceKeyNotFoundException.class);
		assertThat(listAthletePersonalRecordsUseCase.execute(fixture.accountId(), null, null)).isEmpty();
	}

	@Test
	void recomputeRefusesAnExecutionThatIsNotCompleted() {
		Fixture fixture = fixture();
		Session session = session(fixture, LocalDate.of(2026, 4, 6));

		assertThatThrownBy(() -> recomputeWorkoutExerciseExecutionMetricsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(),
				session.occurrenceId(), session.executionId()))
				.isInstanceOf(TrainingMetricsRequireCompletedExecutionException.class);
	}

	@Test
	void recomputeRefusesAnExecutionWhoseSetsWereAllSkipped() {
		Fixture fixture = fixture();
		Session session = session(fixture, LocalDate.of(2026, 4, 6));
		listSets(fixture, session).forEach(set -> skipWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(),
				session.occurrenceId(), session.executionId(), set.id()));
		completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(),
				session.occurrenceId(), session.executionId());

		assertThatThrownBy(() -> recomputeWorkoutExerciseExecutionMetricsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(),
				session.occurrenceId(), session.executionId()))
				.isInstanceOf(TrainingMetricsRequireCompletedSetsException.class);
		assertThat(listAthletePersonalRecordsUseCase.execute(fixture.accountId(), null, null)).isEmpty();
	}

	@Test
	void historyIsPagedNewestSessionFirst() {
		Fixture fixture = fixture();
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 6)), 3, "100", WeightUnit.KILOGRAM);
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 13)), 3, "110", WeightUnit.KILOGRAM);
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 20)), 3, "120", WeightUnit.KILOGRAM);
		ExercisePerformanceKey key = ExercisePerformanceKey.of(fixture.definitionId());

		AthleteExercisePerformanceHistoryResult firstPage = getAthleteExercisePerformanceHistoryUseCase.execute(
				fixture.accountId(), key, null, null, 0, 2);
		AthleteExercisePerformanceHistoryResult secondPage = getAthleteExercisePerformanceHistoryUseCase.execute(
				fixture.accountId(), key, null, null, 1, 2);

		assertThat(firstPage.totalElements()).isEqualTo(3);
		assertThat(firstPage.totalPages()).isEqualTo(2);
		assertThat(firstPage.exerciseName()).isEqualTo(BACK_SQUAT);
		assertThat(firstPage.entries()).extracting(ExerciseExecutionPerformanceResult::scheduledDate)
				.containsExactly(LocalDate.of(2026, 4, 20), LocalDate.of(2026, 4, 13));
		assertThat(secondPage.entries()).extracting(ExerciseExecutionPerformanceResult::scheduledDate)
				.containsExactly(LocalDate.of(2026, 4, 6));
	}

	@Test
	void historyCanBeNarrowedToAScheduledWindow() {
		Fixture fixture = fixture();
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 6)), 3, "100", WeightUnit.KILOGRAM);
		logSingleSet(session(fixture, LocalDate.of(2026, 4, 20)), 3, "120", WeightUnit.KILOGRAM);
		ExercisePerformanceKey key = ExercisePerformanceKey.of(fixture.definitionId());

		AthleteExercisePerformanceHistoryResult window = getAthleteExercisePerformanceHistoryUseCase.execute(
				fixture.accountId(), key, LocalDate.of(2026, 4, 15), LocalDate.of(2026, 4, 30), null, null);

		assertThat(window.entries()).extracting(ExerciseExecutionPerformanceResult::scheduledDate)
				.containsExactly(LocalDate.of(2026, 4, 20));
		assertThatThrownBy(() -> getAthleteExercisePerformanceHistoryUseCase.execute(
				fixture.accountId(), key, LocalDate.of(2026, 4, 30), LocalDate.of(2026, 4, 15), null, null))
				.isInstanceOf(InvalidTrainingPerformanceRangeException.class);
		assertThatThrownBy(() -> getAthleteExercisePerformanceHistoryUseCase.execute(
				fixture.accountId(), key, null, null, 0, 101))
				.isInstanceOf(InvalidTrainingPerformanceRangeException.class);
	}

	@Test
	void personalRecordsCanBeFilteredAndListedByRecency() {
		Fixture fixture = fixture();
		logPoundSession(session(fixture, LocalDate.of(2026, 4, 6)));
		ExercisePerformanceKey key = ExercisePerformanceKey.of(fixture.definitionId());

		assertThat(listAthletePersonalRecordsUseCase.execute(
				fixture.accountId(), key, PersonalRecordType.HEAVIEST_WEIGHT))
				.singleElement()
				.satisfies(record -> assertThat(record.recordType())
						.isEqualTo(PersonalRecordType.HEAVIEST_WEIGHT));
		// Every record was achieved during this test run, so the whole projection is "recent".
		assertThat(getRecentAthletePersonalRecordsUseCase.execute(fixture.accountId(), null, null)).hasSize(6);
		assertThat(getRecentAthletePersonalRecordsUseCase.execute(fixture.accountId(), 366, 2)).hasSize(2);
		assertThatThrownBy(() -> getRecentAthletePersonalRecordsUseCase.execute(fixture.accountId(), 0, null))
				.isInstanceOf(InvalidTrainingPerformanceRangeException.class);
		assertThatThrownBy(() -> getRecentAthletePersonalRecordsUseCase.execute(fixture.accountId(), 367, null))
				.isInstanceOf(InvalidTrainingPerformanceRangeException.class);
		assertThatThrownBy(() -> getRecentAthletePersonalRecordsUseCase.execute(fixture.accountId(), null, 0))
				.isInstanceOf(InvalidTrainingPerformanceRangeException.class);
	}

	private Map<String, PersonalRecordResult> recordsByKey(Fixture fixture) {
		return getAthleteExercisePersonalRecordsUseCase
				.execute(fixture.accountId(), ExercisePerformanceKey.of(fixture.definitionId()))
				.stream()
				.collect(java.util.stream.Collectors.toMap(
						record -> record.recordQualifier() == null
								? record.recordType().name()
								: record.recordType().name() + "|" + record.recordQualifier(),
						Function.identity()));
	}

	private static Map<String, String> comparableValues(Map<String, PersonalRecordResult> records) {
		return records.entrySet().stream().collect(java.util.stream.Collectors.toMap(
				Map.Entry::getKey,
				entry -> entry.getValue().normalizedValue().toPlainString()
						+ "@" + entry.getValue().achievedAt()
						+ "#" + entry.getValue().sourceSetId().value()));
	}

	private List<AthleteExercisePersonalRecordHistory> heaviestWeightHistory(Fixture fixture) {
		return personalRecordHistoryRepository
				.findAllByAthleteIdAndExercisePerformanceKey(
						athleteId(fixture.accountId()), ExercisePerformanceKey.of(fixture.definitionId()))
				.stream()
				.filter(entry -> entry.recordType() == PersonalRecordType.HEAVIEST_WEIGHT)
				.toList();
	}

	private List<String> historyFingerprint(Fixture fixture) {
		return personalRecordHistoryRepository
				.findAllByAthleteIdAndExercisePerformanceKey(
						athleteId(fixture.accountId()), ExercisePerformanceKey.of(fixture.definitionId()))
				.stream()
				.map(entry -> entry.recordType().name()
						+ "|" + entry.recordQualifier()
						+ "|" + entry.measurement().normalizedValue().toPlainString()
						+ "|" + entry.achievedAt()
						+ "|" + entry.sourceSetId().value()
						+ "|" + (entry.isCurrent() ? "current" : "superseded"))
				.toList();
	}

	private void logPoundSession(Session session) {
		List<WorkoutExerciseSetResult> sets = listSets(session.fixture(), session);
		logAndComplete(session, sets.get(0), 5, new BigDecimal("225"), WeightUnit.POUND);
		logAndComplete(session, sets.get(1), 5, new BigDecimal("225"), WeightUnit.POUND);
		logAndComplete(session, sets.get(2), 5, new BigDecimal("225"), WeightUnit.POUND);
		logAndComplete(session, sets.get(3), 4, new BigDecimal("225"), WeightUnit.POUND);
		logAndComplete(session, sets.get(4), 6, new BigDecimal("205"), WeightUnit.POUND);
		finish(session);
	}

	private void logSingleSet(Session session, int reps, String weight, WeightUnit unit) {
		List<WorkoutExerciseSetResult> sets = listSets(session.fixture(), session);
		logAndComplete(session, sets.getFirst(), reps, new BigDecimal(weight), unit);
		sets.stream().skip(1).forEach(set -> skipWorkoutExerciseSetUseCase.execute(
				session.fixture().accountId(), session.fixture().planId(), session.fixture().dayId(),
				session.occurrenceId(), session.executionId(), set.id()));
		finish(session);
	}

	private void finish(Session session) {
		Fixture fixture = session.fixture();
		completeWorkoutExerciseExecutionUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(),
				session.occurrenceId(), session.executionId());
		completeWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), session.occurrenceId());
	}

	private void logAndComplete(
			Session session,
			WorkoutExerciseSetResult set,
			Integer reps,
			BigDecimal weight,
			WeightUnit unit) {
		Fixture fixture = session.fixture();
		updateWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), session.occurrenceId(),
				session.executionId(), set.id(),
				new UpdateWorkoutExerciseSetCommand(
						null, false,
						reps, true,
						weight, true,
						unit, true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false));
		completeWorkoutExerciseSetUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), session.occurrenceId(),
				session.executionId(), set.id());
	}

	private List<WorkoutExerciseSetResult> listSets(Fixture fixture, Session session) {
		return listWorkoutExerciseSetsUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(),
				session.occurrenceId(), session.executionId());
	}

	private Session session(Fixture fixture, LocalDate scheduledDate) {
		WorkoutOccurrenceDetailResult occurrence = createWorkoutOccurrenceUseCase.execute(
				fixture.accountId(), fixture.planId(), fixture.dayId(), scheduledDate, null, null);
		return new Session(fixture, occurrence);
	}

	private Fixture fixture() {
		AccountId accountId = AccountId.generate();
		createAthleteProfileUseCase.execute(
				com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()),
				"Riley",
				"Torres",
				LocalDate.of(1996, 2, 3),
				Sex.MALE,
				Height.ofCentimeters(182),
				Weight.ofKilograms(84),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
		TrainingPlanResult plan = createTrainingPlanUseCase.execute(
				accountId, TrainingPlanType.STRENGTH, null, "Squat Cycle", null,
				LocalDate.of(2026, 3, 1), LocalDate.of(2026, 12, 31), null, null);
		WorkoutDayResult day = createWorkoutDayUseCase.execute(
				accountId, plan.id(), "Lower", null, 1, DayOfWeek.MONDAY, null, null, null);
		ExerciseDefinitionId definitionId = exerciseDefinitions.idFor(accountId, BACK_SQUAT);
		WorkoutExerciseResult exercise = createWorkoutExerciseUseCase.execute(
				accountId, plan.id(), day.id(), definitionId,
				BACK_SQUAT, ExerciseCategory.STRENGTH, ExerciseType.BARBELL,
				5, 5, 5, new BigDecimal("225"), WeightUnit.POUND,
				null, null, null, null, null, null, null, null);
		return new Fixture(accountId, plan.id(), day.id(), exercise.id().value(), definitionId);
	}

	private AthleteId athleteId(AccountId accountId) {
		return AthleteId.of(athleteRepository
				.findByAccountId(com.devinolabs.uap.athlete.domain.AccountId.of(accountId.value()))
				.orElseThrow()
				.id()
				.value());
	}

	private record Fixture(
			AccountId accountId,
			TrainingPlanId planId,
			WorkoutDayId dayId,
			UUID exerciseId,
			ExerciseDefinitionId definitionId) {
	}

	private record Session(Fixture fixture, WorkoutOccurrenceDetailResult occurrence) {

		WorkoutOccurrenceId occurrenceId() {
			return occurrence.occurrence().id();
		}

		WorkoutExerciseExecutionId executionId() {
			return occurrence.executions().getFirst().id();
		}

		ExercisePerformanceKey exercisePerformanceKey() {
			return occurrence.executions().getFirst().exercisePerformanceKey();
		}
	}

}
