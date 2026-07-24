package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoal;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.GoalStatusAction;

@Service
public class ChangeAthleteGoalStatusUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteGoalRepository athleteGoalRepository;
	private final Clock clock;

	public ChangeAthleteGoalStatusUseCase(
			AthleteRepository athleteRepository,
			AthleteGoalRepository athleteGoalRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteGoalRepository = Objects.requireNonNull(athleteGoalRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AthleteGoalResult execute(AccountId accountId, AthleteGoalId goalId, GoalStatusAction action) {
		Athlete athlete = AthleteGoalSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		AthleteGoal goal = athleteGoalRepository.findByIdAndAthleteId(goalId, athlete.id())
				.orElseThrow(AthleteGoalNotFoundException::new);

		try {
			goal.applyStatusAction(action, clock);
		}
		catch (IllegalStateException ex) {
			throw new InvalidAthleteGoalStatusTransitionException(ex.getMessage());
		}

		if (goal.isActiveDuplicateCandidate()) {
			AthleteGoalSupport.assertNoActiveDuplicate(
					athleteGoalRepository,
					athlete,
					goal.goalType(),
					goal.title(),
					goal.id());
		}

		return AthleteGoalSupport.toResult(athleteGoalRepository.save(goal));
	}

}
