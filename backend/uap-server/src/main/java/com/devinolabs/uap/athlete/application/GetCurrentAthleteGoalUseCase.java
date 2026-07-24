package com.devinolabs.uap.athlete.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoal;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;

@Service
public class GetCurrentAthleteGoalUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteGoalRepository athleteGoalRepository;

	public GetCurrentAthleteGoalUseCase(
			AthleteRepository athleteRepository,
			AthleteGoalRepository athleteGoalRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteGoalRepository = Objects.requireNonNull(athleteGoalRepository);
	}

	@Transactional(readOnly = true)
	public AthleteGoalResult execute(AccountId accountId, AthleteGoalId goalId) {
		Athlete athlete = AthleteGoalSupport.requireAthlete(athleteRepository, accountId);
		AthleteGoal goal = athleteGoalRepository.findByIdAndAthleteId(goalId, athlete.id())
				.orElseThrow(AthleteGoalNotFoundException::new);
		return AthleteGoalSupport.toResult(goal);
	}

}
