package com.devinolabs.uap.athlete.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentType;
import com.devinolabs.uap.athlete.domain.Athlete;

@Service
public class ListAssessmentsUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;

	public ListAssessmentsUseCase(AthleteRepository athleteRepository, AssessmentRepository assessmentRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
	}

	@Transactional(readOnly = true)
	public List<AssessmentResult> execute(
			AccountId accountId,
			AssessmentStatus status,
			AssessmentType assessmentType,
			Instant scheduledFrom,
			Instant scheduledTo) {
		if (scheduledFrom != null && scheduledTo != null && scheduledFrom.isAfter(scheduledTo)) {
			throw new InvalidAssessmentDateException("scheduledFrom must not be after scheduledTo");
		}
		Athlete athlete = AssessmentSupport.requireAthlete(athleteRepository, accountId);
		return AssessmentSupport.ordered(assessmentRepository.findFiltered(
				athlete.id(), status, assessmentType, scheduledFrom, scheduledTo));
	}

}
