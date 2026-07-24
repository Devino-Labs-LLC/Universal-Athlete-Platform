package com.devinolabs.uap.athlete.application;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoal;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.AthleteStatus;
import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalTarget;
import com.devinolabs.uap.athlete.domain.GoalType;

final class AthleteGoalSupport {

	private AthleteGoalSupport() {
	}

	static Athlete requireMutableAthlete(AthleteRepository athleteRepository, AccountId accountId) {
		Athlete athlete = athleteRepository.findByAccountId(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);
		assertNotArchived(athlete);
		return athlete;
	}

	/**
	 * Resolves the athlete under a pessimistic write lock so active-duplicate checks
	 * and inserts cannot race for the same account.
	 */
	static Athlete requireMutableAthleteForUpdate(AthleteRepository athleteRepository, AccountId accountId) {
		Athlete athlete = athleteRepository.findByAccountIdForUpdate(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);
		assertNotArchived(athlete);
		return athlete;
	}

	static Athlete requireAthlete(AthleteRepository athleteRepository, AccountId accountId) {
		return athleteRepository.findByAccountId(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);
	}

	private static void assertNotArchived(Athlete athlete) {
		if (athlete.status() == AthleteStatus.ARCHIVED) {
			throw new AthleteArchivedException();
		}
	}

	static void assertLinkedSportBelongsToAthlete(
			AthleteSportRepository sportRepository,
			Athlete athlete,
			AthleteSportId athleteSportId) {
		if (athleteSportId == null) {
			return;
		}
		sportRepository.findByIdAndAthleteId(athleteSportId, athlete.id())
				.orElseThrow(AthleteSportNotFoundException::new);
	}

	static void assertNoActiveDuplicate(
			AthleteGoalRepository goalRepository,
			Athlete athlete,
			GoalType goalType,
			String title,
			AthleteGoalId excludingId) {
		String normalizedTitle = AthleteGoal.normalizeTitle(title);
		if (goalRepository.existsActiveDuplicate(athlete.id(), goalType, normalizedTitle, excludingId)) {
			throw new DuplicateAthleteGoalException();
		}
	}

	static AthleteGoalResult toResult(AthleteGoal goal) {
		GoalTarget target = goal.target();
		return new AthleteGoalResult(
				goal.id(),
				goal.goalType(),
				goal.customGoalName(),
				goal.title(),
				goal.description(),
				goal.priority(),
				goal.status(),
				target == null ? null : target.value(),
				target == null ? null : target.unit(),
				target == null ? null : target.customUnit(),
				goal.targetDate(),
				goal.athleteSportId(),
				goal.createdAt(),
				goal.updatedAt(),
				goal.completedAt());
	}

	static List<AthleteGoalResult> ordered(List<AthleteGoal> goals) {
		return goals.stream()
				.sorted(Comparator
						.comparingInt((AthleteGoal goal) -> statusRank(goal.status()))
						.thenComparing(AthleteGoal::priority, Comparator.reverseOrder())
						.thenComparing(AthleteGoal::targetDate, Comparator.nullsLast(Comparator.naturalOrder()))
						.thenComparing(AthleteGoal::createdAt, Comparator.reverseOrder()))
				.map(AthleteGoalSupport::toResult)
				.toList();
	}

	private static int statusRank(GoalStatus status) {
		return switch (Objects.requireNonNull(status)) {
			case ACTIVE -> 0;
			case PAUSED -> 1;
			case COMPLETED -> 2;
			case CANCELLED -> 3;
		};
	}

	static RuntimeException translateValidation(IllegalArgumentException ex) {
		String message = ex.getMessage() == null ? "" : ex.getMessage();
		if (message.contains("customGoalName")) {
			return new InvalidCustomGoalNameException(message);
		}
		if (message.contains("targetDate")) {
			return new InvalidGoalTargetDateException(message);
		}
		if (message.contains("target") || message.contains("customTargetUnit")) {
			return new InvalidAthleteGoalTargetException(message);
		}
		return ex;
	}

}
