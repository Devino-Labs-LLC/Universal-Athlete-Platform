package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingEnvironment;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

public interface TrainingEnvironmentRepository {

	TrainingEnvironment save(TrainingEnvironment environment);

	Optional<TrainingEnvironment> findById(TrainingEnvironmentId id);

	Optional<TrainingEnvironment> findOwnedById(TrainingEnvironmentId id, AthleteId athleteId);

	Optional<TrainingEnvironment> findActiveDefaultByAthleteId(AthleteId athleteId);

	boolean existsActiveByAthleteIdAndNormalizedName(AthleteId athleteId, String normalizedName);

	boolean existsActiveByAthleteIdAndNormalizedNameExcluding(
			AthleteId athleteId,
			String normalizedName,
			TrainingEnvironmentId excludingId);

	boolean hasAnyActiveByAthleteId(AthleteId athleteId);

	Optional<TrainingEnvironment> findActiveDefaultForUpdate(AthleteId athleteId);

	void clearDefaultForAthleteExcept(AthleteId athleteId, TrainingEnvironmentId keepDefaultId);

	TrainingEnvironmentPage findByAthlete(AthleteId athleteId, TrainingEnvironmentFilters filters, int page, int size);

	List<TrainingEnvironment> findAllActiveByAthleteId(AthleteId athleteId);

}
