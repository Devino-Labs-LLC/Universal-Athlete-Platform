package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurementId;
import com.devinolabs.uap.athlete.domain.Athlete;

@Service
public class DetachMeasurementFromAssessmentUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;
	private final AssessmentMeasurementRepository assessmentMeasurementRepository;
	private final Clock clock;

	public DetachMeasurementFromAssessmentUseCase(
			AthleteRepository athleteRepository,
			AssessmentRepository assessmentRepository,
			AssessmentMeasurementRepository assessmentMeasurementRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.assessmentMeasurementRepository = Objects.requireNonNull(assessmentMeasurementRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void execute(AccountId accountId, AssessmentId assessmentId, AssessmentMeasurementId attachmentId) {
		Athlete athlete = AssessmentSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		Assessment assessment = AssessmentMeasurementSupport.requireMutableAssessment(
				assessmentRepository, athlete, assessmentId);

		AssessmentMeasurement attachment = assessmentMeasurementRepository
				.findByIdAndAssessmentIdAndAthleteId(attachmentId, assessment.id(), athlete.id())
				.orElseThrow(AssessmentMeasurementNotFoundException::new);
		assessmentMeasurementRepository.delete(attachment);

		List<AssessmentMeasurement> remaining = assessmentMeasurementRepository
				.findAllByAssessmentIdAndAthleteId(assessment.id(), athlete.id());
		AssessmentMeasurementSupport.compactDisplayOrders(remaining, assessmentMeasurementRepository, clock);
	}

}
