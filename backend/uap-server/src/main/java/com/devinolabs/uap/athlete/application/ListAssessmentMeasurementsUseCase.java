package com.devinolabs.uap.athlete.application;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;

@Service
public class ListAssessmentMeasurementsUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;
	private final AssessmentMeasurementRepository assessmentMeasurementRepository;
	private final AthleteMeasurementRepository athleteMeasurementRepository;

	public ListAssessmentMeasurementsUseCase(
			AthleteRepository athleteRepository,
			AssessmentRepository assessmentRepository,
			AssessmentMeasurementRepository assessmentMeasurementRepository,
			AthleteMeasurementRepository athleteMeasurementRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.assessmentMeasurementRepository = Objects.requireNonNull(assessmentMeasurementRepository);
		this.athleteMeasurementRepository = Objects.requireNonNull(athleteMeasurementRepository);
	}

	@Transactional(readOnly = true)
	public List<AssessmentMeasurementResult> execute(AccountId accountId, AssessmentId assessmentId) {
		Athlete athlete = AssessmentSupport.requireAthlete(athleteRepository, accountId);
		Assessment assessment = AssessmentMeasurementSupport.requireAssessment(
				assessmentRepository, athlete, assessmentId);
		List<AssessmentMeasurement> attachments = assessmentMeasurementRepository
				.findAllByAssessmentIdAndAthleteId(assessment.id(), athlete.id());

		if (assessment.status() == AssessmentStatus.COMPLETED) {
			return attachments.stream()
					.map(attachment -> AssessmentMeasurementSupport.fromSnapshot(
							attachment,
							attachment.snapshot(),
							true))
					.toList();
		}

		Map<AthleteMeasurementId, AthleteMeasurement> sources = AssessmentMeasurementSupport.loadSourcesOptional(
				athleteMeasurementRepository, athlete, attachments);
		return attachments.stream()
				.map(attachment -> {
					AthleteMeasurement source = sources.get(attachment.sourceMeasurementId());
					if (source == null && attachment.isSnapshotted()) {
						// Prefer current source; if deleted while reopened, fall back to retained snapshot
						// only for measurement fields when listing — preferred rule says return current source.
						// Without source after reopen, expose retained snapshot data with snapshotted=true.
						return AssessmentMeasurementSupport.fromSnapshot(attachment, attachment.snapshot(), true);
					}
					return AssessmentMeasurementSupport.toResult(attachment, assessment, source);
				})
				.toList();
	}

}
