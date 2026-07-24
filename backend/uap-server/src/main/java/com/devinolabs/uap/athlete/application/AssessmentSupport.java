package com.devinolabs.uap.athlete.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentType;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.AthleteStatus;

final class AssessmentSupport {

	private AssessmentSupport() {
	}

	static Athlete requireMutableAthlete(AthleteRepository athleteRepository, AccountId accountId) {
		Athlete athlete = athleteRepository.findByAccountId(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);
		if (athlete.status() == AthleteStatus.ARCHIVED) {
			throw new AthleteArchivedException();
		}
		return athlete;
	}

	static Athlete requireMutableAthleteForUpdate(AthleteRepository athleteRepository, AccountId accountId) {
		Athlete athlete = athleteRepository.findByAccountIdForUpdate(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);
		if (athlete.status() == AthleteStatus.ARCHIVED) {
			throw new AthleteArchivedException();
		}
		return athlete;
	}

	static Athlete requireAthlete(AthleteRepository athleteRepository, AccountId accountId) {
		return athleteRepository.findByAccountId(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);
	}

	static void assertLinkedSport(
			AthleteSportRepository sportRepository,
			Athlete athlete,
			AthleteSportId sportId) {
		if (sportId == null) {
			return;
		}
		sportRepository.findByIdAndAthleteId(sportId, athlete.id())
				.orElseThrow(AthleteSportNotFoundException::new);
	}

	static void assertLinkedGoal(
			AthleteGoalRepository goalRepository,
			Athlete athlete,
			AthleteGoalId goalId) {
		if (goalId == null) {
			return;
		}
		goalRepository.findByIdAndAthleteId(goalId, athlete.id())
				.orElseThrow(AthleteGoalNotFoundException::new);
	}

	static void assertNoDuplicate(
			AssessmentRepository assessmentRepository,
			Athlete athlete,
			AssessmentType type,
			String title,
			Instant scheduledAt,
			AssessmentId excludingId) {
		String normalizedTitle = Assessment.normalizeTitle(title);
		if (assessmentRepository.existsDuplicate(
				athlete.id(), type, normalizedTitle, scheduledAt, excludingId)) {
			throw new DuplicateAssessmentException();
		}
	}

	static AssessmentResult toResult(Assessment assessment) {
		return new AssessmentResult(
				assessment.id(),
				assessment.type(),
				assessment.customTypeName(),
				assessment.title(),
				assessment.description(),
				assessment.status(),
				assessment.scheduledAt(),
				assessment.startedAt(),
				assessment.completedAt(),
				assessment.notes(),
				assessment.athleteSportId(),
				assessment.athleteGoalId(),
				assessment.createdAt(),
				assessment.updatedAt());
	}

	static List<AssessmentResult> ordered(List<Assessment> assessments) {
		return assessments.stream()
				.sorted(Comparator
						.comparing(Assessment::scheduledAt, Comparator.nullsLast(Comparator.reverseOrder()))
						.thenComparing(Assessment::createdAt, Comparator.reverseOrder())
						.thenComparing(assessment -> assessment.id().value()))
				.map(AssessmentSupport::toResult)
				.toList();
	}

	static RuntimeException translateValidation(IllegalArgumentException ex) {
		String message = ex.getMessage() == null ? "" : ex.getMessage();
		if (message.contains("customTypeName")) {
			return new InvalidCustomAssessmentTypeException(message);
		}
		if (message.contains("startedAt") || message.contains("completedAt") || message.contains("scheduled")) {
			return new InvalidAssessmentDateException(message);
		}
		return ex;
	}

}
