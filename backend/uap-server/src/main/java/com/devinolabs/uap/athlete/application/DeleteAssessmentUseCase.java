package com.devinolabs.uap.athlete.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.Athlete;

@Service
public class DeleteAssessmentUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;

	public DeleteAssessmentUseCase(AthleteRepository athleteRepository, AssessmentRepository assessmentRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
	}

	@Transactional
	public void execute(AccountId accountId, AssessmentId assessmentId) {
		Athlete athlete = AssessmentSupport.requireMutableAthlete(athleteRepository, accountId);
		Assessment assessment = assessmentRepository.findByIdAndAthleteId(assessmentId, athlete.id())
				.orElseThrow(AssessmentNotFoundException::new);
		if (assessment.status() != AssessmentStatus.PLANNED && assessment.status() != AssessmentStatus.CANCELLED) {
			throw new AssessmentDeleteNotAllowedException();
		}
		assessmentRepository.delete(assessment);
	}

}
