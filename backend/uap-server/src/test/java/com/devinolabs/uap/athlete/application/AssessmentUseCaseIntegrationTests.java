package com.devinolabs.uap.athlete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentStatusAction;
import com.devinolabs.uap.athlete.domain.AssessmentType;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalType;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;
import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.SportType;
import com.devinolabs.uap.athlete.domain.Weight;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AssessmentUseCaseIntegrationTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private AddAthleteSportUseCase addAthleteSportUseCase;

	@Autowired
	private CreateAthleteGoalUseCase createAthleteGoalUseCase;

	@Autowired
	private CreateAssessmentUseCase createAssessmentUseCase;

	@Autowired
	private ListAssessmentsUseCase listAssessmentsUseCase;

	@Autowired
	private GetAssessmentUseCase getAssessmentUseCase;

	@Autowired
	private UpdateAssessmentUseCase updateAssessmentUseCase;

	@Autowired
	private ChangeAssessmentStatusUseCase changeAssessmentStatusUseCase;

	@Autowired
	private DeleteAssessmentUseCase deleteAssessmentUseCase;

	@Autowired
	private RecordAthleteMeasurementUseCase recordAthleteMeasurementUseCase;

	@Autowired
	private AttachMeasurementToAssessmentUseCase attachMeasurementToAssessmentUseCase;

	@Autowired
	private AthleteRepository athleteRepository;

	@Autowired
	private AssessmentRepository assessmentRepository;

	@Test
	void createsListsFiltersUpdatesAndDeletesAssessments() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteSportResult sport = addAthleteSportUseCase.execute(
				accountId, SportType.BASKETBALL, null, true, ParticipationLevel.COLLEGIATE, null, 5, SeasonStatus.IN_SEASON);
		AthleteGoalResult goal = createAthleteGoalUseCase.execute(
				accountId, GoalType.IMPROVE_STRENGTH, null, "Power", null, GoalPriority.MEDIUM,
				null, null, null, null, sport.id());

		AssessmentResult earlier = createAssessmentUseCase.execute(
				accountId, AssessmentType.VERTICAL_JUMP, null, "Vertical", null,
				Instant.parse("2026-08-01T10:00:00Z"), null, sport.id().value(), goal.id().value());
		AssessmentResult later = createAssessmentUseCase.execute(
				accountId, AssessmentType.STRENGTH, null, "Strength", "baseline",
				Instant.parse("2026-08-10T10:00:00Z"), "notes", null, null);
		createAssessmentUseCase.execute(
				accountId, AssessmentType.MOBILITY, null, "Mobility", null,
				Instant.parse("2026-08-05T10:00:00Z"), null, null, null);

		List<AssessmentResult> ordered = listAssessmentsUseCase.execute(accountId, null, null, null, null);
		assertThat(ordered).extracting(AssessmentResult::scheduledAt)
				.containsExactly(
						Instant.parse("2026-08-10T10:00:00Z"),
						Instant.parse("2026-08-05T10:00:00Z"),
						Instant.parse("2026-08-01T10:00:00Z"));
		assertThat(ordered.get(0).id()).isEqualTo(later.id());
		assertThat(ordered.get(2).id()).isEqualTo(earlier.id());

		assertThat(listAssessmentsUseCase.execute(
				accountId, AssessmentStatus.PLANNED, null, null, null)).hasSize(3);
		assertThat(listAssessmentsUseCase.execute(
				accountId, null, AssessmentType.STRENGTH, null, null)).hasSize(1);
		assertThat(listAssessmentsUseCase.execute(
				accountId, null, null,
				Instant.parse("2026-08-01T00:00:00Z"),
				Instant.parse("2026-08-05T23:59:59Z"))).hasSize(2);

		assertThatThrownBy(() -> listAssessmentsUseCase.execute(
				accountId, null, null,
				Instant.parse("2026-08-10T00:00:00Z"),
				Instant.parse("2026-08-01T00:00:00Z")))
				.isInstanceOf(InvalidAssessmentDateException.class);

		AssessmentResult updated = updateAssessmentUseCase.execute(
				accountId,
				later.id(),
				new UpdateAssessmentCommand(
						"Strength Baseline", true,
						"updated", true,
						null, true,
						Instant.parse("2026-08-11T10:00:00Z"), true,
						sport.id().value(), true,
						goal.id().value(), true));
		assertThat(updated.title()).isEqualTo("Strength Baseline");
		assertThat(updated.description()).isEqualTo("updated");
		assertThat(updated.notes()).isNull();
		assertThat(updated.athleteSportId()).isEqualTo(sport.id());
		assertThat(updated.athleteGoalId()).isEqualTo(goal.id());

		deleteAssessmentUseCase.execute(accountId, earlier.id());
		assertThatThrownBy(() -> getAssessmentUseCase.execute(accountId, earlier.id()))
				.isInstanceOf(AssessmentNotFoundException.class);
	}

	@Test
	void rejectsDuplicatesSupportsStatusLifecycleAndDeleteRules() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		Instant scheduledAt = Instant.parse("2026-08-01T10:00:00Z");

		AssessmentResult created = createAssessmentUseCase.execute(
				accountId, AssessmentType.STRENGTH, null, "  Strength   Baseline ", null, scheduledAt, null, null, null);

		assertThatThrownBy(() -> createAssessmentUseCase.execute(
				accountId, AssessmentType.STRENGTH, null, "strength baseline", null, scheduledAt, null, null, null))
				.isInstanceOf(DuplicateAssessmentException.class);

		AssessmentResult started = changeAssessmentStatusUseCase.execute(
				accountId, created.id(), AssessmentStatusAction.START);
		assertThat(started.status()).isEqualTo(AssessmentStatus.IN_PROGRESS);
		assertThat(started.startedAt()).isNotNull();

		assertThatThrownBy(() -> deleteAssessmentUseCase.execute(accountId, created.id()))
				.isInstanceOf(AssessmentDeleteNotAllowedException.class);

		assertThatThrownBy(() -> changeAssessmentStatusUseCase.execute(
				accountId, created.id(), AssessmentStatusAction.COMPLETE))
				.isInstanceOf(AssessmentCompletionRequiresMeasurementsException.class);

		AthleteMeasurementResult measurement = recordAthleteMeasurementUseCase.execute(
				accountId, MeasurementType.BODY_WEIGHT, null, new BigDecimal("80.0000"), MeasurementUnit.KILOGRAM,
				null, MeasurementSource.MANUAL, null, Instant.parse("2026-07-20T10:00:00Z"), null, null);
		attachMeasurementToAssessmentUseCase.execute(
				accountId, created.id(), measurement.id(), null, null, null);

		AssessmentResult completed = changeAssessmentStatusUseCase.execute(
				accountId, created.id(), AssessmentStatusAction.COMPLETE);
		assertThat(completed.status()).isEqualTo(AssessmentStatus.COMPLETED);
		assertThatThrownBy(() -> deleteAssessmentUseCase.execute(accountId, created.id()))
				.isInstanceOf(AssessmentDeleteNotAllowedException.class);

		AssessmentResult reopened = changeAssessmentStatusUseCase.execute(
				accountId, created.id(), AssessmentStatusAction.REOPEN);
		assertThat(reopened.status()).isEqualTo(AssessmentStatus.IN_PROGRESS);

		changeAssessmentStatusUseCase.execute(accountId, created.id(), AssessmentStatusAction.CANCEL);
		deleteAssessmentUseCase.execute(accountId, created.id());
		assertThatThrownBy(() -> getAssessmentUseCase.execute(accountId, created.id()))
				.isInstanceOf(AssessmentNotFoundException.class);

		AssessmentResult cancelledCreate = createAssessmentUseCase.execute(
				accountId, AssessmentType.STRENGTH, null, "Cancelled Dup", null, scheduledAt, null, null, null);
		changeAssessmentStatusUseCase.execute(accountId, cancelledCreate.id(), AssessmentStatusAction.CANCEL);
		AssessmentResult allowedAfterCancel = createAssessmentUseCase.execute(
				accountId, AssessmentType.STRENGTH, null, "Cancelled Dup", null, scheduledAt, null, null, null);
		assertThat(allowedAfterCancel.id()).isNotEqualTo(cancelledCreate.id());
	}

	@Test
	void rejectsCrossAccountLinksArchivedAthleteAndEnforcesIsolation() {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();
		createAthlete(owner);
		createAthlete(other);

		AthleteSportResult otherSport = addAthleteSportUseCase.execute(
				other, SportType.TENNIS, null, true, ParticipationLevel.BEGINNER, null, 1, SeasonStatus.OFF_SEASON);
		AthleteGoalResult otherGoal = createAthleteGoalUseCase.execute(
				other, GoalType.GENERAL_FITNESS, null, "Fit", null, GoalPriority.LOW,
				null, null, null, null, null);

		assertThatThrownBy(() -> createAssessmentUseCase.execute(
				owner, AssessmentType.STRENGTH, null, "Strength", null, null, null,
				otherSport.id().value(), null)).isInstanceOf(AthleteSportNotFoundException.class);
		assertThatThrownBy(() -> createAssessmentUseCase.execute(
				owner, AssessmentType.STRENGTH, null, "Strength", null, null, null,
				null, otherGoal.id().value())).isInstanceOf(AthleteGoalNotFoundException.class);
		assertThatThrownBy(() -> createAssessmentUseCase.execute(
				owner, AssessmentType.STRENGTH, null, "Strength", null, null, null,
				AthleteSportId.generate().value(), null)).isInstanceOf(AthleteSportNotFoundException.class);

		AssessmentResult ownerAssessment = createAssessmentUseCase.execute(
				owner, AssessmentType.POWER, null, "Power", null, null, null, null, null);
		assertThatThrownBy(() -> getAssessmentUseCase.execute(other, ownerAssessment.id()))
				.isInstanceOf(AssessmentNotFoundException.class);

		Athlete athlete = athleteRepository.findByAccountId(owner).orElseThrow();
		athlete.archive(CLOCK);
		athleteRepository.save(athlete);
		assertThatThrownBy(() -> createAssessmentUseCase.execute(
				owner, AssessmentType.POWER, null, "Power 2", null, null, null, null, null))
				.isInstanceOf(AthleteArchivedException.class);
		assertThatThrownBy(() -> updateAssessmentUseCase.execute(
				owner,
				ownerAssessment.id(),
				new UpdateAssessmentCommand("Renamed", true, null, false, null, false, null, false, null, false, null,
						false)))
				.isInstanceOf(AthleteArchivedException.class);
	}

	@Test
	void mysqlRoundTripEnumsAndOptimisticLocking() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteId athleteId = athleteRepository.findByAccountId(accountId).orElseThrow().id();

		AssessmentResult created = createAssessmentUseCase.execute(
				accountId, AssessmentType.OTHER, "Court Screen", "Custom Assessment", "desc",
				Instant.parse("2026-08-01T10:00:00Z"), "notes", null, null);

		Assessment loaded = assessmentRepository.findByIdAndAthleteId(created.id(), athleteId).orElseThrow();
		assertThat(loaded.type()).isEqualTo(AssessmentType.OTHER);
		assertThat(loaded.customTypeName()).isEqualTo("Court Screen");
		assertThat(loaded.status()).isEqualTo(AssessmentStatus.PLANNED);
		assertThat(loaded.normalizedTitle()).isEqualTo("custom assessment");
		assertThat(loaded.version()).isZero();

		loaded.rename("Custom Assessment Updated", CLOCK);
		Assessment saved = assessmentRepository.save(loaded);
		assertThat(saved.version()).isEqualTo(1L);

		Assessment stale = Assessment.rehydrate(
				loaded.id(),
				loaded.athleteId(),
				loaded.athleteSportId(),
				loaded.athleteGoalId(),
				loaded.type(),
				loaded.customTypeName(),
				"Stale",
				"stale",
				loaded.description(),
				loaded.status(),
				loaded.scheduledAt(),
				loaded.startedAt(),
				loaded.completedAt(),
				loaded.notes(),
				loaded.createdAt(),
				loaded.updatedAt(),
				0L);
		assertThatThrownBy(() -> assessmentRepository.save(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);
	}

	private void createAthlete(AccountId accountId) {
		createAthleteProfileUseCase.execute(
				accountId,
				"Jordan",
				"Lee",
				LocalDate.of(1998, 5, 12),
				Sex.FEMALE,
				Height.ofCentimeters(175),
				Weight.ofKilograms(68),
				DominantHand.RIGHT,
				DominantFoot.RIGHT);
	}

}
