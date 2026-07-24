package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurementId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;

@Service
public class UpdateAssessmentMeasurementUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;
	private final AssessmentMeasurementRepository assessmentMeasurementRepository;
	private final AthleteMeasurementRepository athleteMeasurementRepository;
	private final Clock clock;

	public UpdateAssessmentMeasurementUseCase(
			AthleteRepository athleteRepository,
			AssessmentRepository assessmentRepository,
			AssessmentMeasurementRepository assessmentMeasurementRepository,
			AthleteMeasurementRepository athleteMeasurementRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.assessmentMeasurementRepository = Objects.requireNonNull(assessmentMeasurementRepository);
		this.athleteMeasurementRepository = Objects.requireNonNull(athleteMeasurementRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AssessmentMeasurementResult execute(
			AccountId accountId,
			AssessmentId assessmentId,
			AssessmentMeasurementId attachmentId,
			UpdateAssessmentMeasurementCommand command) {
		Athlete athlete = AssessmentSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		Assessment assessment = AssessmentMeasurementSupport.requireMutableAssessment(
				assessmentRepository, athlete, assessmentId);

		AssessmentMeasurement attachment = assessmentMeasurementRepository
				.findByIdAndAssessmentIdAndAthleteId(attachmentId, assessment.id(), athlete.id())
				.orElseThrow(AssessmentMeasurementNotFoundException::new);

		if (command.displayOrderPresent()) {
			if (command.displayOrder() == null) {
				throw new InvalidAssessmentMeasurementOrderException("displayOrder cannot be null");
			}
			attachment.changeDisplayOrder(command.displayOrder(), clock);
		}
		if (command.labelPresent()) {
			attachment.changeLabel(command.label(), clock);
		}
		if (command.notesPresent()) {
			attachment.changeNotes(command.notes(), clock);
		}

		AssessmentMeasurement saved = assessmentMeasurementRepository.save(attachment);
		AthleteMeasurement source = athleteMeasurementRepository
				.findByIdAndAthleteId(saved.sourceMeasurementId(), athlete.id())
				.orElseThrow(AthleteMeasurementNotFoundException::new);
		return AssessmentMeasurementSupport.fromSource(
				saved,
				source,
				saved.isSnapshotted(),
				saved.snapshot() == null ? null : saved.snapshot().snapshottedAt());
	}

}
