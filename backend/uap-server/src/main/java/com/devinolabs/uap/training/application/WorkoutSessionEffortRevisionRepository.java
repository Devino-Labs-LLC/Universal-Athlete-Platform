package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortRevision;

public interface WorkoutSessionEffortRevisionRepository {

	WorkoutSessionEffortRevision save(WorkoutSessionEffortRevision revision);

	List<WorkoutSessionEffortRevision> findAllByEffortIdAndAthleteIdOrderByRevisionNumber(
			WorkoutSessionEffortId effortId,
			AthleteId athleteId);

	int countByEffortId(WorkoutSessionEffortId effortId);

}
