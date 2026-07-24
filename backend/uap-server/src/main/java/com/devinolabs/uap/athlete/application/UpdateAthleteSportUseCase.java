package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteSport;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.ParticipationLevel;
import com.devinolabs.uap.athlete.domain.SeasonStatus;

@Service
public class UpdateAthleteSportUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteSportRepository athleteSportRepository;
	private final Clock clock;

	public UpdateAthleteSportUseCase(
			AthleteRepository athleteRepository,
			AthleteSportRepository athleteSportRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteSportRepository = Objects.requireNonNull(athleteSportRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AthleteSportResult execute(
			AccountId accountId,
			AthleteSportId sportId,
			ParticipationLevel participationLevel,
			String preferredPosition,
			int yearsExperience,
			SeasonStatus seasonStatus) {
		Athlete athlete = AthleteSportSupport.requireMutableAthlete(athleteRepository, accountId);
		AthleteSport sport = athleteSportRepository.findByIdAndAthleteId(sportId, athlete.id())
				.orElseThrow(AthleteSportNotFoundException::new);
		sport.updateParticipation(participationLevel, preferredPosition, yearsExperience, seasonStatus, clock);
		return AthleteSportSupport.toResult(athleteSportRepository.save(sport));
	}

}
