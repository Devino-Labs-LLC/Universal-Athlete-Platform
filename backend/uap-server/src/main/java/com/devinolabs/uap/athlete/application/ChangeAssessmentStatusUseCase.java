package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentStatusAction;
import com.devinolabs.uap.athlete.domain.Athlete;

@Service
public class ChangeAssessmentStatusUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;
	private final Clock clock;

	public ChangeAssessmentStatusUseCase(
			AthleteRepository athleteRepository,
			AssessmentRepository assessmentRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AssessmentResult execute(AccountId accountId, AssessmentId assessmentId, AssessmentStatusAction action) {
		Athlete athlete = AssessmentSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		Assessment assessment = assessmentRepository.findByIdAndAthleteId(assessmentId, athlete.id())
				.orElseThrow(AssessmentNotFoundException::new);

		try {
			assessment.applyStatusAction(action, clock);
		}
		catch (IllegalStateException ex) {
			throw new InvalidAssessmentStatusException(ex.getMessage());
		}
		catch (IllegalArgumentException ex) {
			throw AssessmentSupport.translateValidation(ex);
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

}
