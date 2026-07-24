package com.devinolabs.uap.athlete.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalType;

@Service
public class ListCurrentAthleteGoalsUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteGoalRepository athleteGoalRepository;

	public ListCurrentAthleteGoalsUseCase(
			AthleteRepository athleteRepository,
			AthleteGoalRepository athleteGoalRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteGoalRepository = Objects.requireNonNull(athleteGoalRepository);
	}

	@Transactional(readOnly = true)
	public List<AthleteGoalResult> execute(AccountId accountId, GoalStatus status, GoalType goalType) {
		Athlete athlete = AthleteGoalSupport.requireAthlete(athleteRepository, accountId);
		return AthleteGoalSupport.ordered(athleteGoalRepository.findByAthleteId(athlete.id(), status, goalType));
	}

}
