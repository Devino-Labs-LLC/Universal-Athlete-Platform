package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DuplicateWorkoutExerciseSetOrderException;
import com.devinolabs.uap.training.application.WorkoutExerciseSetRepository;
import com.devinolabs.uap.training.application.WorkoutExerciseSetStatusCount;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSet;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSetStatus;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Repository
class JpaWorkoutExerciseSetRepository implements WorkoutExerciseSetRepository {

	private final WorkoutExerciseSetJpaRepository jpaRepository;

	JpaWorkoutExerciseSetRepository(WorkoutExerciseSetJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutExerciseSet save(WorkoutExerciseSet set) {
		try {
			boolean isNew = !jpaRepository.existsById(set.id().value());
			WorkoutExerciseSetJpaEntity saved = jpaRepository.save(
					WorkoutExerciseSetPersistenceMapper.toEntity(set, isNew));
			// Flush before mapping so the caller sees the post-write version and can save again
			// within the same transaction (resequencing does exactly that).
			jpaRepository.flush();
			return WorkoutExerciseSetPersistenceMapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public List<WorkoutExerciseSet> saveAll(Collection<WorkoutExerciseSet> sets) {
		try {
			List<WorkoutExerciseSetJpaEntity> entities = new ArrayList<>(sets.size());
			for (WorkoutExerciseSet set : sets) {
				boolean isNew = !jpaRepository.existsById(set.id().value());
				entities.add(WorkoutExerciseSetPersistenceMapper.toEntity(set, isNew));
			}
			List<WorkoutExerciseSetJpaEntity> saved = jpaRepository.saveAll(entities);
			jpaRepository.flush();
			return saved.stream()
					.map(WorkoutExerciseSetPersistenceMapper::toDomain)
					.toList();
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public void delete(WorkoutExerciseSet set) {
		jpaRepository.deleteById(set.id().value());
		jpaRepository.flush();
	}

	@Override
	public Optional<WorkoutExerciseSet> findByIdAndExecutionIdAndAthleteId(
			WorkoutExerciseSetId id,
			WorkoutExerciseExecutionId executionId,
			AthleteId athleteId) {
		return jpaRepository
				.findOwned(id.value(), executionId.value(), athleteId.value())
				.map(WorkoutExerciseSetPersistenceMapper::toDomain);
	}

	@Override
	public List<WorkoutExerciseSet> findAllByExecutionIdAndAthleteId(
			WorkoutExerciseExecutionId executionId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllByWorkoutExerciseExecutionIdAndAthleteIdOrderByDisplayOrderAscSetNumberAscIdAsc(
						executionId.value(), athleteId.value())
				.stream()
				.map(WorkoutExerciseSetPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<WorkoutExerciseSet> findAllByOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllByWorkoutOccurrenceIdAndAthleteIdOrderByDisplayOrderAscSetNumberAscIdAsc(
						occurrenceId.value(), athleteId.value())
				.stream()
				.map(WorkoutExerciseSetPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<WorkoutExerciseSetStatusCount> countByStatusForExecutions(
			Collection<WorkoutExerciseExecutionId> executionIds,
			AthleteId athleteId) {
		if (executionIds.isEmpty()) {
			return List.of();
		}
		return jpaRepository
				.countByStatusForExecutions(
						executionIds.stream().map(WorkoutExerciseExecutionId::value).toList(),
						athleteId.value())
				.stream()
				.map(row -> new WorkoutExerciseSetStatusCount(
						WorkoutExerciseExecutionId.of((UUID) row[0]),
						(WorkoutExerciseSetStatus) row[1],
						((Number) row[2]).longValue()))
				.toList();
	}

	private static RuntimeException mapConstraint(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
		String lower = message == null ? "" : message.toLowerCase();
		if (lower.contains("uq_workout_exercise_sets_execution_order")
				|| lower.contains("uq_workout_exercise_sets_execution_number")) {
			return new DuplicateWorkoutExerciseSetOrderException();
		}
		return ex;
	}

}
