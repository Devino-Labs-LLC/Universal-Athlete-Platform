package com.devinolabs.uap.athlete.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.devinolabs.uap.TestcontainersConfiguration;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentStatusAction;
import com.devinolabs.uap.athlete.domain.AssessmentType;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class AssessmentMeasurementUseCaseIntegrationTests {

	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-07-24T15:00:00Z"), ZoneOffset.UTC);

	@Autowired
	private CreateAthleteProfileUseCase createAthleteProfileUseCase;

	@Autowired
	private CreateAssessmentUseCase createAssessmentUseCase;

	@Autowired
	private ChangeAssessmentStatusUseCase changeAssessmentStatusUseCase;

	@Autowired
	private RecordAthleteMeasurementUseCase recordAthleteMeasurementUseCase;

	@Autowired
	private UpdateAthleteMeasurementUseCase updateAthleteMeasurementUseCase;

	@Autowired
	private DeleteAthleteMeasurementUseCase deleteAthleteMeasurementUseCase;

	@Autowired
	private AttachMeasurementToAssessmentUseCase attachMeasurementToAssessmentUseCase;

	@Autowired
	private ListAssessmentMeasurementsUseCase listAssessmentMeasurementsUseCase;

	@Autowired
	private UpdateAssessmentMeasurementUseCase updateAssessmentMeasurementUseCase;

	@Autowired
	private ReorderAssessmentMeasurementsUseCase reorderAssessmentMeasurementsUseCase;

	@Autowired
	private DetachMeasurementFromAssessmentUseCase detachMeasurementFromAssessmentUseCase;

	@Autowired
	private AthleteRepository athleteRepository;

	@Autowired
	private AssessmentMeasurementRepository assessmentMeasurementRepository;

	@Test
	void attachListUpdateReorderDetachAndOrdering() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AssessmentResult assessment = createAssessmentUseCase.execute(
				accountId, AssessmentType.STRENGTH, null, "Strength", null, null, null, null, null);
		AthleteMeasurementResult m1 = recordWeight(accountId, "80.0000", "2026-07-20T10:00:00Z");
		AthleteMeasurementResult m2 = recordWeight(accountId, "81.0000", "2026-07-21T10:00:00Z");
		AthleteMeasurementResult m3 = recordWeight(accountId, "82.0000", "2026-07-22T10:00:00Z");

		AssessmentMeasurementResult a1 = attachMeasurementToAssessmentUseCase.execute(
				accountId, assessment.id(), m1.id(), null, " first ", " note ");
		assertThat(a1.displayOrder()).isZero();
		assertThat(a1.label()).isEqualTo("first");
		assertThat(a1.notes()).isEqualTo("note");
		assertThat(a1.snapshotted()).isFalse();
		assertThat(a1.value()).isEqualByComparingTo("80.0000");

		AssessmentMeasurementResult a2 = attachMeasurementToAssessmentUseCase.execute(
				accountId, assessment.id(), m2.id(), 5, null, null);
		assertThat(a2.displayOrder()).isEqualTo(5);

		AssessmentMeasurementResult a3 = attachMeasurementToAssessmentUseCase.execute(
				accountId, assessment.id(), m3.id(), null, null, null);
		assertThat(a3.displayOrder()).isEqualTo(6);

		assertThatThrownBy(() -> attachMeasurementToAssessmentUseCase.execute(
				accountId, assessment.id(), m1.id(), null, null, null))
				.isInstanceOf(DuplicateAssessmentMeasurementException.class);

		List<AssessmentMeasurementResult> reordered = reorderAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id(), List.of(a3.id().value(), a1.id().value(), a2.id().value()));
		assertThat(reordered).extracting(AssessmentMeasurementResult::id)
				.containsExactly(a3.id(), a1.id(), a2.id());
		assertThat(reordered).extracting(AssessmentMeasurementResult::displayOrder)
				.containsExactly(0, 1, 2);

		AssessmentMeasurementResult patched = updateAssessmentMeasurementUseCase.execute(
				accountId,
				assessment.id(),
				a1.id(),
				new UpdateAssessmentMeasurementCommand(null, false, null, true, "updated", true));
		assertThat(patched.label()).isNull();
		assertThat(patched.notes()).isEqualTo("updated");
		assertThat(patched.displayOrder()).isEqualTo(1);

		assertThatThrownBy(() -> updateAssessmentMeasurementUseCase.execute(
				accountId,
				assessment.id(),
				a1.id(),
				new UpdateAssessmentMeasurementCommand(null, true, null, false, null, false)))
				.isInstanceOf(InvalidAssessmentMeasurementOrderException.class);

		detachMeasurementFromAssessmentUseCase.execute(accountId, assessment.id(), a2.id());
		List<AssessmentMeasurementResult> afterDetach = listAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id());
		assertThat(afterDetach).extracting(AssessmentMeasurementResult::id).containsExactly(a3.id(), a1.id());
		assertThat(afterDetach).extracting(AssessmentMeasurementResult::displayOrder).containsExactly(0, 1);

		assertThatThrownBy(() -> reorderAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id(), List.of(a3.id().value())))
				.isInstanceOf(InvalidAssessmentMeasurementOrderException.class);
		assertThatThrownBy(() -> reorderAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id(), List.of(a3.id().value(), a1.id().value(), a1.id().value())))
				.isInstanceOf(InvalidAssessmentMeasurementOrderException.class);
		assertThatThrownBy(() -> reorderAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id(), List.of(a3.id().value(), UUID.randomUUID())))
				.isInstanceOf(InvalidAssessmentMeasurementOrderException.class);
	}

	@Test
	void completionSnapshotsReopenAndSourceDeletionPolicy() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AssessmentResult assessment = createAssessmentUseCase.execute(
				accountId, AssessmentType.POWER, null, "Power", null, null, null, null, null);
		AthleteMeasurementResult measurement = recordWeight(accountId, "80.1234", "2026-07-20T10:00:00Z");
		AssessmentMeasurementResult attached = attachMeasurementToAssessmentUseCase.execute(
				accountId, assessment.id(), measurement.id(), null, null, null);

		assertThatThrownBy(() -> deleteAthleteMeasurementUseCase.execute(accountId, measurement.id()))
				.isInstanceOf(AthleteMeasurementInUseByAssessmentException.class);

		AssessmentResult emptyAssessment = createAssessmentUseCase.execute(
				accountId, AssessmentType.AGILITY, null, "Empty", null, null, null, null, null);
		changeAssessmentStatusUseCase.execute(accountId, emptyAssessment.id(), AssessmentStatusAction.START);
		assertThatThrownBy(() -> changeAssessmentStatusUseCase.execute(
				accountId, emptyAssessment.id(), AssessmentStatusAction.COMPLETE))
				.isInstanceOf(AssessmentCompletionRequiresMeasurementsException.class);

		changeAssessmentStatusUseCase.execute(accountId, assessment.id(), AssessmentStatusAction.START);
		AssessmentResult completed = changeAssessmentStatusUseCase.execute(
				accountId, assessment.id(), AssessmentStatusAction.COMPLETE);
		assertThat(completed.status()).isEqualTo(AssessmentStatus.COMPLETED);

		List<AssessmentMeasurementResult> snapshotted = listAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id());
		assertThat(snapshotted).hasSize(1);
		assertThat(snapshotted.getFirst().snapshotted()).isTrue();
		assertThat(snapshotted.getFirst().value()).isEqualByComparingTo("80.1234");
		assertThat(snapshotted.getFirst().snapshottedAt()).isNotNull();

		updateAthleteMeasurementUseCase.execute(
				accountId,
				measurement.id(),
				new UpdateAthleteMeasurementCommand(
						new BigDecimal("70.0000"), true,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false,
						null, false));
		List<AssessmentMeasurementResult> afterCorrection = listAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id());
		assertThat(afterCorrection.getFirst().value()).isEqualByComparingTo("80.1234");

		deleteAthleteMeasurementUseCase.execute(accountId, measurement.id());
		List<AssessmentMeasurementResult> afterSourceDelete = listAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id());
		assertThat(afterSourceDelete.getFirst().value()).isEqualByComparingTo("80.1234");
		assertThat(afterSourceDelete.getFirst().snapshotted()).isTrue();

		assertThatThrownBy(() -> attachMeasurementToAssessmentUseCase.execute(
				accountId, assessment.id(), measurement.id(), null, null, null))
				.isInstanceOf(AssessmentMeasurementModificationNotAllowedException.class);

		changeAssessmentStatusUseCase.execute(accountId, assessment.id(), AssessmentStatusAction.REOPEN);
		AthleteMeasurementResult replacement = recordWeight(accountId, "75.5000", "2026-07-23T10:00:00Z");
		detachMeasurementFromAssessmentUseCase.execute(accountId, assessment.id(), attached.id());
		AssessmentMeasurementResult reattached = attachMeasurementToAssessmentUseCase.execute(
				accountId, assessment.id(), replacement.id(), null, null, null);
		List<AssessmentMeasurementResult> reopenedList = listAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id());
		assertThat(reopenedList.getFirst().value()).isEqualByComparingTo("75.5000");
		assertThat(reopenedList.getFirst().snapshotted()).isFalse();

		changeAssessmentStatusUseCase.execute(accountId, assessment.id(), AssessmentStatusAction.COMPLETE);
		List<AssessmentMeasurementResult> recommpleted = listAssessmentMeasurementsUseCase.execute(
				accountId, assessment.id());
		assertThat(recommpleted.getFirst().id()).isEqualTo(reattached.id());
		assertThat(recommpleted.getFirst().value()).isEqualByComparingTo("75.5000");
		assertThat(recommpleted.getFirst().snapshotted()).isTrue();
	}

	@Test
	void rejectsCrossAccountAndCancelledAndCompletedModifications() {
		AccountId owner = AccountId.generate();
		AccountId other = AccountId.generate();
		createAthlete(owner);
		createAthlete(other);

		AssessmentResult ownerAssessment = createAssessmentUseCase.execute(
				owner, AssessmentType.MOBILITY, null, "Mobility", null, null, null, null, null);
		AthleteMeasurementResult ownerMeasurement = recordWeight(owner, "70.0000", "2026-07-20T10:00:00Z");
		AthleteMeasurementResult otherMeasurement = recordWeight(other, "71.0000", "2026-07-20T10:00:00Z");
		AssessmentResult otherAssessment = createAssessmentUseCase.execute(
				other, AssessmentType.MOBILITY, null, "Other", null, null, null, null, null);

		assertThatThrownBy(() -> attachMeasurementToAssessmentUseCase.execute(
				owner, otherAssessment.id(), ownerMeasurement.id(), null, null, null))
				.isInstanceOf(AssessmentNotFoundException.class);
		assertThatThrownBy(() -> attachMeasurementToAssessmentUseCase.execute(
				owner, ownerAssessment.id(), otherMeasurement.id(), null, null, null))
				.isInstanceOf(AthleteMeasurementNotFoundException.class);

		AssessmentMeasurementResult attached = attachMeasurementToAssessmentUseCase.execute(
				owner, ownerAssessment.id(), ownerMeasurement.id(), null, null, null);

		changeAssessmentStatusUseCase.execute(owner, ownerAssessment.id(), AssessmentStatusAction.CANCEL);
		assertThatThrownBy(() -> attachMeasurementToAssessmentUseCase.execute(
				owner, ownerAssessment.id(), ownerMeasurement.id(), null, null, null))
				.isInstanceOf(AssessmentMeasurementModificationNotAllowedException.class);
		assertThatThrownBy(() -> deleteAthleteMeasurementUseCase.execute(owner, ownerMeasurement.id()))
				.isInstanceOf(AthleteMeasurementInUseByAssessmentException.class);

		changeAssessmentStatusUseCase.execute(owner, ownerAssessment.id(), AssessmentStatusAction.REOPEN);
		changeAssessmentStatusUseCase.execute(owner, ownerAssessment.id(), AssessmentStatusAction.START);
		changeAssessmentStatusUseCase.execute(owner, ownerAssessment.id(), AssessmentStatusAction.COMPLETE);

		assertThatThrownBy(() -> detachMeasurementFromAssessmentUseCase.execute(
				owner, ownerAssessment.id(), attached.id()))
				.isInstanceOf(AssessmentMeasurementModificationNotAllowedException.class);
		assertThatThrownBy(() -> listAssessmentMeasurementsUseCase.execute(other, ownerAssessment.id()))
				.isInstanceOf(AssessmentNotFoundException.class);
	}

	@Test
	void mysqlRoundTripAndOptimisticLocking() {
		AccountId accountId = AccountId.generate();
		createAthlete(accountId);
		AthleteId athleteId = athleteRepository.findByAccountId(accountId).orElseThrow().id();
		AssessmentResult assessment = createAssessmentUseCase.execute(
				accountId, AssessmentType.OTHER, "Court", "Custom", null, null, null, null, null);
		AthleteMeasurementResult measurement = recordWeight(accountId, "48.1234", "2026-07-20T10:00:00Z");
		AssessmentMeasurementResult created = attachMeasurementToAssessmentUseCase.execute(
				accountId, assessment.id(), measurement.id(), 0, "label", "notes");

		AssessmentMeasurement loaded = assessmentMeasurementRepository
				.findByIdAndAssessmentIdAndAthleteId(created.id(), assessment.id(), athleteId)
				.orElseThrow();
		assertThat(loaded.displayOrder()).isZero();
		assertThat(loaded.isSnapshotted()).isFalse();
		assertThat(loaded.version()).isZero();

		loaded.changeLabel("updated", CLOCK);
		AssessmentMeasurement saved = assessmentMeasurementRepository.save(loaded);
		assertThat(saved.version()).isEqualTo(1L);

		AssessmentMeasurement stale = AssessmentMeasurement.rehydrate(
				loaded.id(),
				loaded.assessmentId(),
				loaded.athleteId(),
				loaded.sourceMeasurementId(),
				loaded.displayOrder(),
				"stale",
				loaded.notes(),
				null,
				loaded.createdAt(),
				loaded.updatedAt(),
				0L);
		assertThatThrownBy(() -> assessmentMeasurementRepository.save(stale))
				.isInstanceOf(ObjectOptimisticLockingFailureException.class);

		changeAssessmentStatusUseCase.execute(accountId, assessment.id(), AssessmentStatusAction.START);
		changeAssessmentStatusUseCase.execute(accountId, assessment.id(), AssessmentStatusAction.COMPLETE);
		AssessmentMeasurement snapshotted = assessmentMeasurementRepository
				.findByIdAndAssessmentIdAndAthleteId(created.id(), assessment.id(), athleteId)
				.orElseThrow();
		assertThat(snapshotted.isSnapshotted()).isTrue();
		assertThat(snapshotted.snapshot().value()).isEqualByComparingTo("48.1234");
		assertThat(snapshotted.snapshot().value().scale()).isEqualTo(4);
	}

	private AthleteMeasurementResult recordWeight(AccountId accountId, String value, String measuredAt) {
		return recordAthleteMeasurementUseCase.execute(
				accountId,
				MeasurementType.BODY_WEIGHT,
				null,
				new BigDecimal(value),
				MeasurementUnit.KILOGRAM,
				null,
				MeasurementSource.MANUAL,
				null,
				Instant.parse(measuredAt),
				null,
				null);
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
