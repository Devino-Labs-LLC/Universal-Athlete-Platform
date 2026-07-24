package com.devinolabs.uap.athlete.application;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteSport;
import com.devinolabs.uap.athlete.domain.AthleteStatus;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.SportType;

final class AthleteSportSupport {

	private AthleteSportSupport() {
	}

	static Athlete requireMutableAthlete(AthleteRepository athleteRepository, AccountId accountId) {
		Athlete athlete = athleteRepository.findByAccountId(accountId)
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

	static AthleteSportResult toResult(AthleteSport sport) {
		return new AthleteSportResult(
				sport.id(),
				sport.sportType(),
				sport.customSportName(),
				sport.primarySport(),
				sport.participationLevel(),
				sport.preferredPosition(),
				sport.yearsExperience(),
				sport.seasonStatus(),
				sport.createdAt(),
				sport.updatedAt());
	}

	static List<AthleteSportResult> ordered(List<AthleteSport> sports) {
		return sports.stream()
				.sorted(Comparator
						.comparing(AthleteSport::primarySport).reversed()
						.thenComparing(sport -> sport.displayName().toLowerCase(Locale.ROOT)))
				.map(AthleteSportSupport::toResult)
				.toList();
	}

	static void assertNoDuplicate(
			AthleteSportRepository sportRepository,
			Athlete athlete,
			SportType sportType,
			String customSportName) {
		if (sportType == SportType.OTHER) {
			String normalized = Objects.requireNonNull(customSportName).trim().toLowerCase(Locale.ROOT);
			if (sportRepository.existsByAthleteIdAndOtherSportNormalized(athlete.id(), normalized)) {
				throw new DuplicateAthleteSportException();
			}
			return;
		}
		if (sportRepository.existsByAthleteIdAndSportType(athlete.id(), sportType)) {
			throw new DuplicateAthleteSportException();
		}
	}

	static void assertCanBecomePrimary(
			AthleteSportRepository sportRepository,
			Athlete athlete,
			boolean requestedPrimary) {
		if (!requestedPrimary) {
			return;
		}
		if (sportRepository.findPrimaryByAthleteId(athlete.id()).isPresent()) {
			throw new PrimaryAthleteSportConflictException();
		}
	}

}
