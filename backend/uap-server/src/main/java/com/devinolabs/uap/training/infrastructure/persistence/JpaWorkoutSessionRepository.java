package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.WorkoutSessionAlreadyExistsException;
import com.devinolabs.uap.training.application.WorkoutSessionRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutExerciseId;
import com.devinolabs.uap.training.domain.WorkoutSession;

@Repository
class JpaWorkoutSessionRepository implements WorkoutSessionRepository {

	private final WorkoutSessionJpaRepository jpaRepository;

	JpaWorkoutSessionRepository(WorkoutSessionJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutSession save(WorkoutSession session) {
		try {
			boolean isNew = !jpaRepository.existsById(session.id().value());
			WorkoutSessionJpaEntity saved = jpaRepository.save(
					WorkoutSessionPersistenceMapper.toEntity(session, isNew));
			return WorkoutSessionPersistenceMapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public Optional<WorkoutSession> findByWorkoutExerciseIdAndAthleteId(
			WorkoutExerciseId workoutExerciseId,
			AthleteId athleteId) {
		return jpaRepository
				.findByWorkoutExerciseIdAndAthleteId(workoutExerciseId.value(), athleteId.value())
				.map(WorkoutSessionPersistenceMapper::toDomain);
	}

	@Override
	public Optional<WorkoutSession> findByWorkoutExerciseIdAndWorkoutDayIdAndAthleteId(
			WorkoutExerciseId workoutExerciseId,
			WorkoutDayId workoutDayId,
			AthleteId athleteId) {
		return jpaRepository
				.findByWorkoutExerciseIdAndWorkoutDayIdAndAthleteId(
						workoutExerciseId.value(),
						workoutDayId.value(),
						athleteId.value())
				.map(WorkoutSessionPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsByWorkoutExerciseId(WorkoutExerciseId workoutExerciseId) {
		return jpaRepository.existsByWorkoutExerciseId(workoutExerciseId.value());
	}

	private static RuntimeException mapConstraint(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
		String lower = message == null ? "" : message.toLowerCase();
		if (lower.contains("uq_workout_sessions_exercise")) {
			return new WorkoutSessionAlreadyExistsException();
		}
		return ex;
	}

}
