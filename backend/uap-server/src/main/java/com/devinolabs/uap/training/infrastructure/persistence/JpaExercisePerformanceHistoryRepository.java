package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.ExercisePerformanceExecutionPage;
import com.devinolabs.uap.training.application.ExercisePerformanceExecutionRow;
import com.devinolabs.uap.training.application.ExercisePerformanceHistoryRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@Repository
class JpaExercisePerformanceHistoryRepository implements ExercisePerformanceHistoryRepository {

	private final WorkoutExerciseExecutionJpaRepository jpaRepository;

	JpaExercisePerformanceHistoryRepository(WorkoutExerciseExecutionJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public ExercisePerformanceExecutionPage findCompletedExecutions(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			LocalDate scheduledFrom,
			LocalDate scheduledTo,
			int page,
			int size) {
		Page<Object[]> rows = jpaRepository.findCompletedPerformanceRows(
				athleteId.value(),
				exercisePerformanceKey.value(),
				scheduledFrom,
				scheduledTo,
				PageRequest.of(page, size));
		return new ExercisePerformanceExecutionPage(
				rows.getContent().stream()
						.map(JpaExercisePerformanceHistoryRepository::toRow)
						.toList(),
				page,
				size,
				rows.getTotalElements());
	}

	@Override
	public List<ExercisePerformanceExecutionRow> findEligibleExecutionsChronologically(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey) {
		return jpaRepository
				.findEligiblePerformanceRowsChronologically(
						athleteId.value(),
						exercisePerformanceKey == null ? null : exercisePerformanceKey.value())
				.stream()
				.map(JpaExercisePerformanceHistoryRepository::toRow)
				.toList();
	}

	@Override
	public boolean existsByAthleteIdAndExercisePerformanceKey(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey) {
		return jpaRepository.existsByAthleteIdAndExercisePerformanceKey(
				athleteId.value(), exercisePerformanceKey.value());
	}

	static ExercisePerformanceExecutionRow toRow(Object[] row) {
		return new ExercisePerformanceExecutionRow(
				WorkoutExerciseExecutionPersistenceMapper.toDomain((WorkoutExerciseExecutionJpaEntity) row[0]),
				(LocalDate) row[1],
				(WorkoutOccurrenceStatus) row[2]);
	}

}
