package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;

public interface TrainingPlanRepository {

	TrainingPlan save(TrainingPlan plan);

	Optional<TrainingPlan> findByIdAndAthleteId(TrainingPlanId id, AthleteId athleteId);

	List<TrainingPlan> findAllByAthleteId(AthleteId athleteId);

	List<TrainingPlan> findFiltered(AthleteId athleteId, TrainingPlanStatus status, TrainingPlanType planType);

	boolean existsOverlappingDuplicate(
			AthleteId athleteId,
			String normalizedName,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId excludingId);

	void delete(TrainingPlan plan);

}
