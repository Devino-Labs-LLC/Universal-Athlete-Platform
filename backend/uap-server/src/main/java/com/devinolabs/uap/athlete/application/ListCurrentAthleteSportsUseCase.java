package com.devinolabs.uap.athlete.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;

@Service
public class ListCurrentAthleteSportsUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteSportRepository athleteSportRepository;

	public ListCurrentAthleteSportsUseCase(
			AthleteRepository athleteRepository,
			AthleteSportRepository athleteSportRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteSportRepository = Objects.requireNonNull(athleteSportRepository);
	}

	@Transactional(readOnly = true)
	public List<AthleteSportResult> execute(AccountId accountId) {
		Athlete athlete = AthleteSportSupport.requireAthlete(athleteRepository, accountId);
		return AthleteSportSupport.ordered(athleteSportRepository.findAllByAthleteId(athlete.id()));
	}

}
