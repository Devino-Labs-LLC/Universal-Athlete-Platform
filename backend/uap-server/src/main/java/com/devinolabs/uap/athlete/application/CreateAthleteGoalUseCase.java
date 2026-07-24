package com.devinolabs.uap.athlete.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoal;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.GoalPriority;
import com.devinolabs.uap.athlete.domain.GoalTarget;
import com.devinolabs.uap.athlete.domain.GoalTargetUnit;
import com.devinolabs.uap.athlete.domain.GoalType;

@Service
public class CreateAthleteGoalUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteGoalRepository athleteGoalRepository;
	private final AthleteSportRepository athleteSportRepository;
	private final Clock clock;

	public CreateAthleteGoalUseCase(
			AthleteRepository athleteRepository,
			AthleteGoalRepository athleteGoalRepository,
			AthleteSportRepository athleteSportRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteGoalRepository = Objects.requireNonNull(athleteGoalRepository);
		this.athleteSportRepository = Objects.requireNonNull(athleteSportRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AthleteGoalResult execute(
			AccountId accountId,
			GoalType goalType,
			String customGoalName,
			String title,
			String description,
			GoalPriority priority,
			BigDecimal targetValue,
			GoalTargetUnit targetUnit,
			String customTargetUnit,
			LocalDate targetDate,
			AthleteSportId athleteSportId) {
		// Serialize create against the athlete row so concurrent duplicates cannot both pass exists().
		Athlete athlete = AthleteGoalSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		AthleteGoalSupport.assertLinkedSportBelongsToAthlete(athleteSportRepository, athlete, athleteSportId);
		AthleteGoalSupport.assertNoActiveDuplicate(athleteGoalRepository, athlete, goalType, title, null);

		try {
			GoalTarget target = GoalTarget.optional(targetValue, targetUnit, customTargetUnit);
			AthleteGoal goal = AthleteGoal.create(
					AthleteGoalId.generate(),
					athlete.id(),
					goalType,
					customGoalName,
					title,
					description,
					priority,
					target,
					targetDate,
					athleteSportId,
					clock);
			return AthleteGoalSupport.toResult(athleteGoalRepository.save(goal));
		}
		catch (IllegalArgumentException ex) {
			throw AthleteGoalSupport.translateValidation(ex);
		}
	}

}
