package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;

@Service
public class UpdateAssessmentUseCase {

	private final AthleteRepository athleteRepository;
	private final AssessmentRepository assessmentRepository;
	private final AthleteSportRepository athleteSportRepository;
	private final AthleteGoalRepository athleteGoalRepository;
	private final Clock clock;

	public UpdateAssessmentUseCase(
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
	public AssessmentResult execute(AccountId accountId, AssessmentId assessmentId, UpdateAssessmentCommand command) {
		Athlete athlete = AssessmentSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		Assessment assessment = assessmentRepository.findByIdAndAthleteId(assessmentId, athlete.id())
				.orElseThrow(AssessmentNotFoundException::new);

		if (command.athleteSportIdPresent()) {
			AthleteSportId sportId = command.athleteSportId() == null
					? null
					: AthleteSportId.of(command.athleteSportId());
			AssessmentSupport.assertLinkedSport(athleteSportRepository, athlete, sportId);
		}
		if (command.athleteGoalIdPresent()) {
			AthleteGoalId goalId = command.athleteGoalId() == null
					? null
					: AthleteGoalId.of(command.athleteGoalId());
			AssessmentSupport.assertLinkedGoal(athleteGoalRepository, athlete, goalId);
		}

		String titleForDuplicate = command.titlePresent() ? command.title() : assessment.title();
		Instant scheduledForDuplicate = command.scheduledAtPresent() ? command.scheduledAt() : assessment.scheduledAt();
		if (assessment.isDuplicateCandidate()) {
			AssessmentSupport.assertNoDuplicate(
					assessmentRepository,
					athlete,
					assessment.type(),
					titleForDuplicate,
					scheduledForDuplicate,
					assessment.id());
		}

		try {
			if (command.titlePresent()) {
				if (command.title() == null || command.title().isBlank()) {
					throw new IllegalArgumentException("title must not be blank");
				}
				assessment.rename(command.title(), clock);
			}
			if (command.descriptionPresent()) {
				assessment.changeDescription(command.description(), clock);
			}
			if (command.notesPresent()) {
				assessment.changeNotes(command.notes(), clock);
			}
			if (command.scheduledAtPresent()) {
				assessment.schedule(command.scheduledAt(), clock);
			}
			if (command.athleteSportIdPresent()) {
				if (command.athleteSportId() == null) {
					assessment.unlinkSport(clock);
				}
				else {
					assessment.linkSport(AthleteSportId.of(command.athleteSportId()), clock);
				}
			}
			if (command.athleteGoalIdPresent()) {
				if (command.athleteGoalId() == null) {
					assessment.unlinkGoal(clock);
				}
				else {
					assessment.linkGoal(AthleteGoalId.of(command.athleteGoalId()), clock);
				}
			}
		}
		catch (IllegalArgumentException ex) {
			throw AssessmentSupport.translateValidation(ex);
		}

		return AssessmentSupport.toResult(assessmentRepository.save(assessment));
	}

}
