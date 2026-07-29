package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.WorkoutExerciseSubstitutionHistoryRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutExerciseExecutionId;
import com.devinolabs.uap.training.domain.WorkoutExerciseSubstitutionHistory;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;

@Repository
class JpaWorkoutExerciseSubstitutionHistoryRepository implements WorkoutExerciseSubstitutionHistoryRepository {

	private final WorkoutExerciseSubstitutionHistoryJpaRepository jpaRepository;

	JpaWorkoutExerciseSubstitutionHistoryRepository(WorkoutExerciseSubstitutionHistoryJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutExerciseSubstitutionHistory append(WorkoutExerciseSubstitutionHistory entry) {
		WorkoutExerciseSubstitutionHistoryJpaEntity saved = jpaRepository.save(
				WorkoutExerciseSubstitutionHistoryPersistenceMapper.toEntity(entry, true));
		jpaRepository.flush();
		return WorkoutExerciseSubstitutionHistoryPersistenceMapper.toDomain(saved);
	}

	@Override
	public List<WorkoutExerciseSubstitutionHistory> findAllByExecutionIdAndAthleteId(
			WorkoutExerciseExecutionId executionId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllForExecution(executionId.value(), athleteId.value())
				.stream()
				.map(WorkoutExerciseSubstitutionHistoryPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<WorkoutExerciseSubstitutionHistory> findAllByOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllForOccurrence(occurrenceId.value(), athleteId.value())
				.stream()
				.map(WorkoutExerciseSubstitutionHistoryPersistenceMapper::toDomain)
				.toList();
	}

}
