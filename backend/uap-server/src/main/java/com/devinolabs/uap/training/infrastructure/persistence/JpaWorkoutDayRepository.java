package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DuplicateWorkoutDayException;
import com.devinolabs.uap.training.application.InvalidWorkoutDayOrderException;
import com.devinolabs.uap.training.application.WorkoutDayRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDay;
import com.devinolabs.uap.training.domain.WorkoutDayId;

@Repository
class JpaWorkoutDayRepository implements WorkoutDayRepository {

	private final WorkoutDayJpaRepository jpaRepository;

	JpaWorkoutDayRepository(WorkoutDayJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutDay save(WorkoutDay day) {
		try {
			boolean isNew = !jpaRepository.existsById(day.id().value());
			WorkoutDayJpaEntity saved = jpaRepository.save(WorkoutDayPersistenceMapper.toEntity(day, isNew));
			return WorkoutDayPersistenceMapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public List<WorkoutDay> saveAll(Collection<WorkoutDay> days) {
		try {
			List<WorkoutDayJpaEntity> entities = new ArrayList<>(days.size());
			for (WorkoutDay day : days) {
				boolean isNew = !jpaRepository.existsById(day.id().value());
				entities.add(WorkoutDayPersistenceMapper.toEntity(day, isNew));
			}
			return jpaRepository.saveAll(entities).stream()
					.map(WorkoutDayPersistenceMapper::toDomain)
					.toList();
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public Optional<WorkoutDay> findByIdAndTrainingPlanIdAndAthleteId(
			WorkoutDayId id,
			TrainingPlanId trainingPlanId,
			AthleteId athleteId) {
		return jpaRepository
				.findByIdAndTrainingPlanIdAndAthleteId(id.value(), trainingPlanId.value(), athleteId.value())
				.map(WorkoutDayPersistenceMapper::toDomain);
	}

	@Override
	public List<WorkoutDay> findAllByTrainingPlanIdAndAthleteId(
			TrainingPlanId trainingPlanId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllByTrainingPlanIdAndAthleteIdOrderByDisplayOrderAscCreatedAtAscIdAsc(
						trainingPlanId.value(),
						athleteId.value())
				.stream()
				.map(WorkoutDayPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByTrainingPlanIdAndNormalizedTitle(TrainingPlanId trainingPlanId, String normalizedTitle) {
		return jpaRepository.existsByTrainingPlanIdAndNormalizedTitle(trainingPlanId.value(), normalizedTitle);
	}

	@Override
	public boolean existsByTrainingPlanIdAndNormalizedTitleExcluding(
			TrainingPlanId trainingPlanId,
			String normalizedTitle,
			WorkoutDayId excludingId) {
		return jpaRepository.existsByTrainingPlanIdAndNormalizedTitleAndIdNot(
				trainingPlanId.value(),
				normalizedTitle,
				excludingId.value());
	}

	@Override
	public int findMaxDisplayOrder(TrainingPlanId trainingPlanId, AthleteId athleteId) {
		return jpaRepository.findMaxDisplayOrder(trainingPlanId.value(), athleteId.value());
	}

	@Override
	public List<WorkoutDay> findAllByTrainingPlanIdWithDisplayOrderAtLeast(
			TrainingPlanId trainingPlanId,
			int displayOrder) {
		return jpaRepository
				.findAllByTrainingPlanIdAndDisplayOrderGreaterThanEqualOrderByDisplayOrderDesc(
						trainingPlanId.value(),
						displayOrder)
				.stream()
				.map(WorkoutDayPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public void delete(WorkoutDay day) {
		jpaRepository.deleteById(day.id().value());
	}

	private static RuntimeException mapConstraint(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
		String lower = message == null ? "" : message.toLowerCase();
		if (lower.contains("uq_workout_days_plan_title")) {
			return new DuplicateWorkoutDayException();
		}
		if (lower.contains("uq_workout_days_plan_order")) {
			return new InvalidWorkoutDayOrderException("displayOrder conflicts with an existing workout day");
		}
		return ex;
	}

}
