package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentType;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;

@Service
public class CreateAssessmentUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;
	private final AthleteSportRepository athleteSportRepository;
	private final AthleteGoalRepository athleteGoalRepository;
	private final Clock clock;

	public CreateAssessmentUseCase(
			AthleteRepository athleteRepository,
			AssessmentRepository assessmentRepository,
			AthleteSportRepository athleteSportRepository,
			AthleteGoalRepository athleteGoalRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.athleteSportRepository = Objects.requireNonNull(athleteSportRepository);
		this.athleteGoalRepository = Objects.requireNonNull(athleteGoalRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AssessmentResult execute(
			AccountId accountId,
			AssessmentType type,
			String customTypeName,
			String title,
			String description,
			Instant scheduledAt,
			String notes,
			UUID athleteSportId,
			UUID athleteGoalId) {
		Athlete athlete = AssessmentSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		AthleteSportId sportId = athleteSportId == null ? null : AthleteSportId.of(athleteSportId);
		AthleteGoalId goalId = athleteGoalId == null ? null : AthleteGoalId.of(athleteGoalId);
		AssessmentSupport.assertLinkedSport(athleteSportRepository, athlete, sportId);
		AssessmentSupport.assertLinkedGoal(athleteGoalRepository, athlete, goalId);
		AssessmentSupport.assertNoDuplicate(assessmentRepository, athlete, type, title, scheduledAt, null);

		try {
			Assessment assessment = Assessment.create(
					AssessmentId.generate(),
					athlete.id(),
					type,
					customTypeName,
					title,
					description,
					scheduledAt,
					notes,
					sportId,
					goalId,
					clock);
			return AssessmentSupport.toResult(assessmentRepository.save(assessment));
		}
		catch (IllegalArgumentException ex) {
			throw AssessmentSupport.translateValidation(ex);
		}
	}

}
