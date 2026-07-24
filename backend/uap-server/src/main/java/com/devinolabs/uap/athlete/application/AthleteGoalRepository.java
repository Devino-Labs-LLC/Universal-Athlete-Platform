package com.devinolabs.uap.athlete.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.athlete.domain.AthleteGoal;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalType;

public interface AthleteGoalRepository {

	AthleteGoal save(AthleteGoal goal);

	Optional<AthleteGoal> findByIdAndAthleteId(AthleteGoalId id, AthleteId athleteId);

	List<AthleteGoal> findByAthleteId(AthleteId athleteId, GoalStatus status, GoalType goalType);

	boolean existsActiveDuplicate(AthleteId athleteId, GoalType goalType, String normalizedTitle, AthleteGoalId excludingId);

	void delete(AthleteGoal goal);

}
