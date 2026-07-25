package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DuplicateWorkoutExerciseException;
import com.devinolabs.uap.training.application.InvalidWorkoutExerciseOrderException;
import com.devinolabs.uap.training.application.WorkoutExerciseRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExercise;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;

@Repository
class JpaWorkoutExerciseRepository implements WorkoutExerciseRepository {

	private final WorkoutExerciseJpaRepository jpaRepository;

	JpaWorkoutExerciseRepository(WorkoutExerciseJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutExercise save(WorkoutExercise exercise) {
		try {
			boolean isNew = !jpaRepository.existsById(exercise.id().value());
			WorkoutExerciseJpaEntity saved = jpaRepository.save(
					WorkoutExercisePersistenceMapper.toEntity(exercise, isNew));
			return WorkoutExercisePersistenceMapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public List<WorkoutExercise> saveAll(Collection<WorkoutExercise> exercises) {
		try {
			List<WorkoutExerciseJpaEntity> entities = new ArrayList<>(exercises.size());
			for (WorkoutExercise exercise : exercises) {
				boolean isNew = !jpaRepository.existsById(exercise.id().value());
				entities.add(WorkoutExercisePersistenceMapper.toEntity(exercise, isNew));
			}
			return jpaRepository.saveAll(entities).stream()
					.map(WorkoutExercisePersistenceMapper::toDomain)
					.toList();
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public Optional<WorkoutExercise> findByIdAndWorkoutDayIdAndAthleteId(
			WorkoutExerciseId id,
			WorkoutDayId workoutDayId,
			AthleteId athleteId) {
		return jpaRepository
				.findByIdAndWorkoutDayIdAndAthleteId(id.value(), workoutDayId.value(), athleteId.value())
				.map(WorkoutExercisePersistenceMapper::toDomain);
	}

	@Override
	public List<WorkoutExercise> findAllByWorkoutDayIdAndAthleteId(
			WorkoutDayId workoutDayId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllByWorkoutDayIdAndAthleteIdOrderByDisplayOrderAscCreatedAtAscIdAsc(
						workoutDayId.value(),
						athleteId.value())
				.stream()
				.map(WorkoutExercisePersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByWorkoutDayIdAndNormalizedExerciseName(
			WorkoutDayId workoutDayId,
			String normalizedExerciseName) {
		return jpaRepository.existsByWorkoutDayIdAndNormalizedExerciseName(
				workoutDayId.value(),
				normalizedExerciseName);
	}

	@Override
	public boolean existsByWorkoutDayIdAndNormalizedExerciseNameExcluding(
			WorkoutDayId workoutDayId,
			String normalizedExerciseName,
			WorkoutExerciseId excludingId) {
		return jpaRepository.existsByWorkoutDayIdAndNormalizedExerciseNameAndIdNot(
				workoutDayId.value(),
				normalizedExerciseName,
				excludingId.value());
	}

	@Override
	public int findMaxDisplayOrder(WorkoutDayId workoutDayId, AthleteId athleteId) {
		return jpaRepository.findMaxDisplayOrder(workoutDayId.value(), athleteId.value());
	}

	@Override
	public List<WorkoutExercise> findAllByWorkoutDayIdWithDisplayOrderAtLeast(
			WorkoutDayId workoutDayId,
			int displayOrder) {
		return jpaRepository
				.findAllByWorkoutDayIdAndDisplayOrderGreaterThanEqualOrderByDisplayOrderDesc(
						workoutDayId.value(),
						displayOrder)
				.stream()
				.map(WorkoutExercisePersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public void delete(WorkoutExercise exercise) {
		jpaRepository.deleteById(exercise.id().value());
	}

	private static RuntimeException mapConstraint(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
		String lower = message == null ? "" : message.toLowerCase();
		if (lower.contains("uq_workout_exercises_day_name")) {
			return new DuplicateWorkoutExerciseException();
		}
		if (lower.contains("uq_workout_exercises_day_order")) {
			return new InvalidWorkoutExerciseOrderException(
					"displayOrder conflicts with an existing workout exercise");
		}
		return ex;
	}

}
