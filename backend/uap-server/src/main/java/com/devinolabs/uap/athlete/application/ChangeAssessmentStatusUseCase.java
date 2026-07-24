package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentStatusAction;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;

@Service
public class ChangeAssessmentStatusUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;
	private final AssessmentMeasurementRepository assessmentMeasurementRepository;
	private final AthleteMeasurementRepository athleteMeasurementRepository;
	private final Clock clock;

	public ChangeAssessmentStatusUseCase(
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
	public AssessmentResult execute(AccountId accountId, AssessmentId assessmentId, AssessmentStatusAction action) {
		Athlete athlete = AssessmentSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		Assessment assessment = assessmentRepository.findByIdAndAthleteId(assessmentId, athlete.id())
				.orElseThrow(AssessmentNotFoundException::new);

		if (action == AssessmentStatusAction.COMPLETE) {
			completeWithSnapshots(athlete, assessment);
		}
		else {
			try {
				assessment.applyStatusAction(action, clock);
			}
			catch (IllegalStateException ex) {
				throw new InvalidAssessmentStatusException(ex.getMessage());
			}
			catch (IllegalArgumentException ex) {
				throw AssessmentSupport.translateValidation(ex);
			}
		}

		if (assessment.isDuplicateCandidate()) {
			AssessmentSupport.assertNoDuplicate(
					assessmentRepository,
					athlete,
					assessment.type(),
					assessment.title(),
					assessment.scheduledAt(),
					assessment.id());
		}

		return AssessmentSupport.toResult(assessmentRepository.save(assessment));
	}

	private void completeWithSnapshots(Athlete athlete, Assessment assessment) {
		try {
			if (assessment.status() != com.devinolabs.uap.athlete.domain.AssessmentStatus.IN_PROGRESS) {
				throw new IllegalStateException("Only IN_PROGRESS assessments can be completed");
			}
		}
		catch (IllegalStateException ex) {
			throw new InvalidAssessmentStatusException(ex.getMessage());
		}

		List<AssessmentMeasurement> attachments = assessmentMeasurementRepository
				.findAllByAssessmentIdAndAthleteId(assessment.id(), athlete.id());
		if (attachments.isEmpty()) {
			throw new AssessmentCompletionRequiresMeasurementsException();
		}

		Map<AthleteMeasurementId, AthleteMeasurement> sources;
		try {
			sources = AssessmentMeasurementSupport.loadSources(
					athleteMeasurementRepository, athlete, attachments);
		}
		catch (AssessmentSnapshotFailedException ex) {
			throw ex;
		}

		for (AssessmentMeasurement attachment : attachments) {
			AthleteMeasurement source = sources.get(attachment.sourceMeasurementId());
			if (source == null) {
				throw new AssessmentSnapshotFailedException("Source measurement is missing and cannot be snapshotted");
			}
			attachment.captureSnapshot(source, clock);
		}
		assessmentMeasurementRepository.saveAll(attachments);

		try {
			assessment.complete(clock);
		}
		catch (IllegalStateException ex) {
			throw new InvalidAssessmentStatusException(ex.getMessage());
		}
		catch (IllegalArgumentException ex) {
			throw AssessmentSupport.translateValidation(ex);
		}
	}

}
