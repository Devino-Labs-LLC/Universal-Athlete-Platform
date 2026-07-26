package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DuplicateWorkoutExerciseExecutionException;
import com.devinolabs.uap.training.application.WorkoutExerciseExecutionRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecution;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Repository
class JpaWorkoutExerciseExecutionRepository implements WorkoutExerciseExecutionRepository {

	private final WorkoutExerciseExecutionJpaRepository jpaRepository;

	JpaWorkoutExerciseExecutionRepository(WorkoutExerciseExecutionJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutExerciseExecution save(WorkoutExerciseExecution execution) {
		try {
			boolean isNew = !jpaRepository.existsById(execution.id().value());
			WorkoutExerciseExecutionJpaEntity saved = jpaRepository.save(
					WorkoutExerciseExecutionPersistenceMapper.toEntity(execution, isNew));
			return WorkoutExerciseExecutionPersistenceMapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public List<WorkoutExerciseExecution> saveAll(Collection<WorkoutExerciseExecution> executions) {
		try {
			List<WorkoutExerciseExecutionJpaEntity> entities = new ArrayList<>(executions.size());
			for (WorkoutExerciseExecution execution : executions) {
				boolean isNew = !jpaRepository.existsById(execution.id().value());
				entities.add(WorkoutExerciseExecutionPersistenceMapper.toEntity(execution, isNew));
			}
			return jpaRepository.saveAll(entities).stream()
					.map(WorkoutExerciseExecutionPersistenceMapper::toDomain)
					.toList();
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public Optional<WorkoutExerciseExecution> findByIdAndWorkoutOccurrenceIdAndAthleteId(
			WorkoutExerciseExecutionId id,
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId) {
		return jpaRepository
				.findByIdAndOccurrenceIdAndAthleteId(id.value(), occurrenceId.value(), athleteId.value())
				.map(WorkoutExerciseExecutionPersistenceMapper::toDomain);
	}

	@Override
	public List<WorkoutExerciseExecution> findAllByWorkoutOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllByOccurrenceIdAndAthleteId(occurrenceId.value(), athleteId.value())
				.stream()
				.map(WorkoutExerciseExecutionPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public Optional<WorkoutExerciseExecution> findByIdAndWorkoutDayIdAndAthleteId(
			WorkoutExerciseExecutionId id,
			WorkoutDayId dayId,
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId) {
		return jpaRepository
				.findOwned(id.value(), occurrenceId.value(), dayId.value(), athleteId.value())
				.map(WorkoutExerciseExecutionPersistenceMapper::toDomain);
	}

	private static RuntimeException mapConstraint(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
		String lower = message == null ? "" : message.toLowerCase();
		if (lower.contains("uq_workout_exercise_executions_occurrence_source")
				|| lower.contains("uq_workout_exercise_executions_occurrence_order")) {
			return new DuplicateWorkoutExerciseExecutionException();
		}
		return ex;
	}

}
