package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingEnvironment;
import com.devinolabs.uap.training.domain.TrainingEnvironmentArchivedException;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceEnvironmentSnapshot;

final class TrainingEnvironmentSupport {

	static final int DEFAULT_PAGE_SIZE = 20;
	static final int MAX_PAGE_SIZE = 100;

	private TrainingEnvironmentSupport() {
	}

	static AthleteRef requireMutableAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireMutableAthleteForUpdate(accountId);
	}

	static AthleteRef requireAthlete(AthleteContextPort athleteContextPort, UUID accountId) {
		return athleteContextPort.requireAthlete(accountId);
	}

	static TrainingEnvironment requireOwnedActive(
			TrainingEnvironmentRepository repository,
			AthleteId athleteId,
			TrainingEnvironmentId environmentId) {
		TrainingEnvironment environment = repository
				.findOwnedById(environmentId, athleteId)
				.orElseThrow(TrainingEnvironmentNotFoundException::new);
		if (!environment.active()) {
			throw new TrainingEnvironmentArchivedException();
		}
		return environment;
	}

	static TrainingEnvironment requireOwned(
			TrainingEnvironmentRepository repository,
			AthleteId athleteId,
			TrainingEnvironmentId environmentId) {
		return repository
				.findOwnedById(environmentId, athleteId)
				.orElseThrow(TrainingEnvironmentNotFoundException::new);
	}

	static void assertNoActiveDuplicate(
			TrainingEnvironmentRepository repository,
			AthleteId athleteId,
			String name,
			TrainingEnvironmentId excludingId) {
		String normalized = TrainingEnvironment.normalizeName(name);
		boolean exists = excludingId == null
				? repository.existsActiveByAthleteIdAndNormalizedName(athleteId, normalized)
				: repository.existsActiveByAthleteIdAndNormalizedNameExcluding(
						athleteId, normalized, excludingId);
		if (exists) {
			throw new DuplicateTrainingEnvironmentException();
		}
	}

	static TrainingEnvironmentResult toResult(TrainingEnvironment environment) {
		return new TrainingEnvironmentResult(
				environment.id(),
				environment.athleteId(),
				environment.name(),
				environment.type(),
				environment.availableEquipment(),
				environment.description(),
				environment.facilityNotes(),
				environment.defaultEnvironment(),
				environment.active(),
				environment.archivedAt(),
				environment.createdAt(),
				environment.updatedAt());
	}

	static WorkoutOccurrenceEnvironmentSnapshot resolvePreferredSnapshot(
			TrainingEnvironmentRepository repository,
			WorkoutDay day,
			TrainingPlan plan,
			AthleteId athleteId) {
		TrainingEnvironmentId overrideId = day.trainingEnvironmentOverrideId();
		if (overrideId != null) {
			TrainingEnvironment override = repository.findOwnedById(overrideId, athleteId).orElse(null);
			if (override != null && override.active()) {
				return WorkoutOccurrenceEnvironmentSnapshot.from(override);
			}
		}
		TrainingEnvironmentId planDefaultId = plan.defaultTrainingEnvironmentId();
		if (planDefaultId != null) {
			TrainingEnvironment planDefault = repository.findOwnedById(planDefaultId, athleteId).orElse(null);
			if (planDefault != null && planDefault.active()) {
				return WorkoutOccurrenceEnvironmentSnapshot.from(planDefault);
			}
		}
		return repository.findActiveDefaultByAthleteId(athleteId)
				.map(WorkoutOccurrenceEnvironmentSnapshot::from)
				.orElse(null);
	}

	static int normalizePage(Integer page) {
		return page == null || page < 0 ? 0 : page;
	}

	static int normalizeSize(Integer size) {
		if (size == null || size <= 0) {
			return DEFAULT_PAGE_SIZE;
		}
		return Math.min(size, MAX_PAGE_SIZE);
	}

	static void applyDefaultSelection(
			TrainingEnvironmentRepository repository,
			TrainingEnvironment environment,
			boolean makeDefault,
			java.time.Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (makeDefault) {
			repository.clearDefaultForAthleteExcept(environment.athleteId(), environment.id());
			environment.markDefault(clock);
			return;
		}
		if (environment.defaultEnvironment()) {
			environment.clearDefault(clock);
		}
	}

}
