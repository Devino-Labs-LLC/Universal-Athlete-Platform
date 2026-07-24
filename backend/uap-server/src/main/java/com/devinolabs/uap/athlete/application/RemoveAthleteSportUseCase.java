package com.devinolabs.uap.athlete.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteSport;
import com.devinolabs.uap.athlete.domain.AthleteSportId;

@Service
public class RemoveAthleteSportUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteSportRepository athleteSportRepository;

	public RemoveAthleteSportUseCase(
			AthleteRepository athleteRepository,
			AthleteSportRepository athleteSportRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteSportRepository = Objects.requireNonNull(athleteSportRepository);
	}

	@Transactional
	public void execute(AccountId accountId, AthleteSportId sportId) {
		Athlete athlete = AthleteSportSupport.requireMutableAthlete(athleteRepository, accountId);
		AthleteSport sport = athleteSportRepository.findByIdAndAthleteId(sportId, athlete.id())
				.orElseThrow(AthleteSportNotFoundException::new);
		athleteSportRepository.delete(sport);
	}

}
