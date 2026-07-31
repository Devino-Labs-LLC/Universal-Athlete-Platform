package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.WorkoutSessionEffortAlreadyExistsException;
import com.devinolabs.uap.training.application.WorkoutSessionEffortRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffort;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortId;

@Repository
class JpaWorkoutSessionEffortRepository implements WorkoutSessionEffortRepository {

	private final WorkoutSessionEffortJpaRepository jpaRepository;

	JpaWorkoutSessionEffortRepository(WorkoutSessionEffortJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutSessionEffort save(WorkoutSessionEffort effort) {
		try {
			boolean isNew = !jpaRepository.existsById(effort.id().value());
			WorkoutSessionEffortJpaEntity entity = jpaRepository.findById(effort.id().value())
					.map(existing -> {
						WorkoutSessionEffortPersistenceMapper.applyMutableFields(existing, effort);
						return existing;
					})
					.orElseGet(() -> WorkoutSessionEffortPersistenceMapper.toEntity(effort, isNew));
			return WorkoutSessionEffortPersistenceMapper.toDomain(jpaRepository.save(entity));
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public Optional<WorkoutSessionEffort> findByOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId) {
		return jpaRepository.findByWorkoutOccurrenceIdAndAthleteId(occurrenceId.value(), athleteId.value())
				.map(WorkoutSessionEffortPersistenceMapper::toDomain);
	}

	@Override
	public Optional<WorkoutSessionEffort> findByIdAndAthleteId(WorkoutSessionEffortId id, AthleteId athleteId) {
		return jpaRepository.findById(id.value())
				.filter(entity -> entity.getAthleteId().equals(athleteId.value()))
				.map(WorkoutSessionEffortPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsByOccurrenceId(WorkoutOccurrenceId occurrenceId) {
		return jpaRepository.existsByWorkoutOccurrenceId(occurrenceId.value());
	}

	private static RuntimeException mapConstraint(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
		if (message != null && message.contains("uq_workout_session_efforts_occurrence")) {
			return new WorkoutSessionEffortAlreadyExistsException();
		}
		return ex;
	}

}
