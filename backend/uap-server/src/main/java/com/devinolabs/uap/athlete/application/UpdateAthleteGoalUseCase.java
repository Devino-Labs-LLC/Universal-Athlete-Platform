package com.devinolabs.uap.athlete.application;

import java.math.BigDecimal;
import java.time.Clock;
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

@Service
public class UpdateAthleteGoalUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteGoalRepository athleteGoalRepository;
	private final AthleteSportRepository athleteSportRepository;
	private final Clock clock;

	public UpdateAthleteGoalUseCase(
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
	public AthleteGoalResult execute(AccountId accountId, AthleteGoalId goalId, UpdateAthleteGoalCommand command) {
		Athlete athlete = AthleteGoalSupport.requireMutableAthleteForUpdate(athleteRepository, accountId);
		AthleteGoal goal = athleteGoalRepository.findByIdAndAthleteId(goalId, athlete.id())
				.orElseThrow(AthleteGoalNotFoundException::new);

		String title = command.titlePresent() ? command.title() : goal.title();
		String description = command.descriptionPresent() ? command.description() : goal.description();
		GoalPriority priority = command.priorityPresent() ? command.priority() : goal.priority();
		if (priority == null) {
			throw new IllegalArgumentException("priority must not be null");
		}

		AthleteSportId linkedSport = goal.athleteSportId();
		if (command.athleteSportIdPresent()) {
			linkedSport = command.athleteSportId() == null ? null : AthleteSportId.of(command.athleteSportId());
			AthleteGoalSupport.assertLinkedSportBelongsToAthlete(athleteSportRepository, athlete, linkedSport);
		}

		try {
			AthleteGoalSupport.assertNoActiveDuplicate(
					athleteGoalRepository,
					athlete,
					goal.goalType(),
					title,
					goal.id());
			goal.updateDetails(title, description, priority, clock);
			if (command.targetValuePresent() || command.targetUnitPresent() || command.customTargetUnitPresent()) {
				BigDecimal value = command.targetValuePresent()
						? command.targetValue()
						: (goal.target() == null ? null : goal.target().value());
				GoalTargetUnit unit = command.targetUnitPresent()
						? command.targetUnit()
						: (goal.target() == null ? null : goal.target().unit());
				String customUnit = command.customTargetUnitPresent()
						? command.customTargetUnit()
						: (goal.target() == null ? null : goal.target().customUnit());
				goal.updateTarget(GoalTarget.optional(value, unit, customUnit), clock);
			}
			if (command.targetDatePresent()) {
				goal.updateTargetDate(command.targetDate(), clock);
			}
			if (command.athleteSportIdPresent()) {
				if (linkedSport == null) {
					goal.unlinkSport(clock);
				}
				else {
					goal.linkSport(linkedSport, clock);
				}
			}
		}
		catch (IllegalStateException ex) {
			throw new TerminalAthleteGoalModificationException();
		}
		catch (IllegalArgumentException ex) {
			throw AthleteGoalSupport.translateValidation(ex);
		}

		return AthleteGoalSupport.toResult(athleteGoalRepository.save(goal));
	}

}
