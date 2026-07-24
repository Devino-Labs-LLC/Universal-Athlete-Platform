package com.devinolabs.uap.athlete.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.Athlete;

@Service
public class GetAssessmentUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;

	public GetAssessmentUseCase(AthleteRepository athleteRepository, AssessmentRepository assessmentRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
	}

	@Transactional(readOnly = true)
	public AssessmentResult execute(AccountId accountId, AssessmentId assessmentId) {
		Athlete athlete = AssessmentSupport.requireAthlete(athleteRepository, accountId);
		Assessment assessment = assessmentRepository.findByIdAndAthleteId(assessmentId, athlete.id())
				.orElseThrow(AssessmentNotFoundException::new);
		return AssessmentSupport.toResult(assessment);
	}

}
