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
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;

@Service
public class AttachMeasurementToAssessmentUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;
	private final AssessmentMeasurementRepository assessmentMeasurementRepository;
	private final AthleteMeasurementRepository athleteMeasurementRepository;
	private final Clock clock;

	public AttachMeasurementToAssessmentUseCase(
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
			AthleteMeasurementId measurementId,
			Integer displayOrder,
			String label,
			String notes) {
		Athlete athlete = AssessmentSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		Assessment assessment = AssessmentMeasurementSupport.requireMutableAssessment(
				assessmentRepository, athlete, assessmentId);

		AthleteMeasurement source = athleteMeasurementRepository.findByIdAndAthleteId(measurementId, athlete.id())
				.orElseThrow(AthleteMeasurementNotFoundException::new);

		if (assessmentMeasurementRepository.existsByAssessmentIdAndSourceMeasurementId(
				assessment.id(), source.id())) {
			throw new DuplicateAssessmentMeasurementException();
		}

		int order;
		if (displayOrder != null) {
			if (displayOrder < 0) {
				throw new InvalidAssessmentMeasurementOrderException("displayOrder must not be negative");
			}
			order = displayOrder;
		}
		else {
			order = assessmentMeasurementRepository.findMaxDisplayOrder(assessment.id(), athlete.id()) + 1;
		}

		AssessmentMeasurement attached = AssessmentMeasurement.attach(
				AssessmentMeasurementId.generate(),
				assessment.id(),
				athlete.id(),
				source.id(),
				order,
				label,
				notes,
				clock);
		AssessmentMeasurement saved = assessmentMeasurementRepository.save(attached);
		return AssessmentMeasurementSupport.fromSource(saved, source, false, null);
	}

}
