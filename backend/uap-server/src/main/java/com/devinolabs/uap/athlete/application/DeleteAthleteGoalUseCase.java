package com.devinolabs.uap.athlete.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoal;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.GoalStatus;

@Service
public class DeleteAthleteGoalUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteGoalRepository athleteGoalRepository;

	public DeleteAthleteGoalUseCase(
			AthleteRepository athleteRepository,
			AthleteGoalRepository athleteGoalRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteGoalRepository = Objects.requireNonNull(athleteGoalRepository);
	}

	@Transactional
	public void execute(AccountId accountId, AthleteGoalId goalId) {
		Athlete athlete = AthleteGoalSupport.requireMutableAthlete(athleteRepository, accountId);
		AthleteGoal goal = athleteGoalRepository.findByIdAndAthleteId(goalId, athlete.id())
				.orElseThrow(AthleteGoalNotFoundException::new);
		if (goal.status() != GoalStatus.CANCELLED) {
			throw new AthleteGoalDeleteRequiresCancelledException();
		}
		athleteGoalRepository.delete(goal);
	}

}
