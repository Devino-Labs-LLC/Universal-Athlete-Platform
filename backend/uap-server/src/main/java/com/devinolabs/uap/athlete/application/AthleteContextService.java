package com.devinolabs.uap.athlete.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.athlete.api.AthleteArchivedException;
import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteGoalNotOwnedException;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.athlete.api.AthleteSportNotOwnedException;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.AthleteStatus;

@Service
class AthleteContextService implements AthleteContextPort {

	private final AthleteRepository athleteRepository;
	private final AthleteSportRepository athleteSportRepository;
	private final AthleteGoalRepository athleteGoalRepository;

	AthleteContextService(
			AthleteRepository athleteRepository,
			AthleteSportRepository athleteSportRepository,
			AthleteGoalRepository athleteGoalRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteSportRepository = Objects.requireNonNull(athleteSportRepository);
		this.athleteGoalRepository = Objects.requireNonNull(athleteGoalRepository);
	}

	@Override
	public AthleteRef requireMutableAthleteForUpdate(UUID accountId) {
		Athlete athlete = athleteRepository.findByAccountIdForUpdate(AccountId.of(accountId))
				.orElseThrow(AthleteNotFoundException::new);
		if (athlete.status() == AthleteStatus.ARCHIVED) {
			throw new AthleteArchivedException();
		}
		return new AthleteRef(athlete.id().value());
	}

	@Override
	public AthleteRef requireAthlete(UUID accountId) {
		Athlete athlete = athleteRepository.findByAccountId(AccountId.of(accountId))
				.orElseThrow(AthleteNotFoundException::new);
		return new AthleteRef(athlete.id().value());
	}

	@Override
	public void assertOptionalSportOwned(UUID athleteId, UUID sportId) {
		if (sportId == null) {
			return;
		}
		athleteSportRepository.findByIdAndAthleteId(AthleteSportId.of(sportId), AthleteId.of(athleteId))
				.orElseThrow(AthleteSportNotOwnedException::new);
	}

	@Override
	public void assertOptionalGoalOwned(UUID athleteId, UUID goalId) {
		if (goalId == null) {
			return;
		}
		athleteGoalRepository.findByIdAndAthleteId(AthleteGoalId.of(goalId), AthleteId.of(athleteId))
				.orElseThrow(AthleteGoalNotOwnedException::new);
	}

}
