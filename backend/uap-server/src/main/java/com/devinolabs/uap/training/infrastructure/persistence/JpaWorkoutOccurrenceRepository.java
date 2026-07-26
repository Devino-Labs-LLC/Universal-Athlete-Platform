package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DuplicateWorkoutOccurrenceException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceGenerationConflictException;
import com.devinolabs.uap.training.application.WorkoutOccurrenceRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutDayId;
import com.devinolabs.uap.training.domain.WorkoutGenerationKey;
import com.devinolabs.uap.training.domain.WorkoutOccurrence;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceOrigin;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@Repository
class JpaWorkoutOccurrenceRepository implements WorkoutOccurrenceRepository {

	private final WorkoutOccurrenceJpaRepository jpaRepository;

	JpaWorkoutOccurrenceRepository(WorkoutOccurrenceJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutOccurrence save(WorkoutOccurrence occurrence) {
		try {
			boolean isNew = !jpaRepository.existsById(occurrence.id().value());
			WorkoutOccurrenceJpaEntity saved = jpaRepository.save(
					WorkoutOccurrencePersistenceMapper.toEntity(occurrence, isNew));
			return WorkoutOccurrencePersistenceMapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public Optional<WorkoutOccurrence> findByIdAndWorkoutDayIdAndAthleteId(
			WorkoutOccurrenceId id,
			WorkoutDayId workoutDayId,
			AthleteId athleteId) {
		return jpaRepository
				.findByIdAndWorkoutDayIdAndAthleteId(id.value(), workoutDayId.value(), athleteId.value())
				.map(WorkoutOccurrencePersistenceMapper::toDomain);
	}

	@Override
	public List<WorkoutOccurrence> findAllByWorkoutDayIdAndAthleteId(
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			WorkoutOccurrenceStatus status,
			LocalDate scheduledFrom,
			LocalDate scheduledTo) {
		return jpaRepository
				.findFiltered(workoutDayId.value(), athleteId.value(), status, scheduledFrom, scheduledTo)
				.stream()
				.map(WorkoutOccurrencePersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByWorkoutDayIdAndAthleteIdAndScheduledDateAndStatusNot(
			WorkoutDayId workoutDayId,
			AthleteId athleteId,
			LocalDate scheduledDate,
			WorkoutOccurrenceStatus excludedStatus) {
		return jpaRepository.existsByWorkoutDayIdAndAthleteIdAndScheduledDateAndStatusNot(
				workoutDayId.value(), athleteId.value(), scheduledDate, excludedStatus);
	}

	@Override
	public Optional<WorkoutOccurrence> findByGenerationKey(WorkoutGenerationKey generationKey) {
		return jpaRepository
				.findByGenerationKey(generationKey.value())
				.map(WorkoutOccurrencePersistenceMapper::toDomain);
	}

	@Override
	public boolean existsByGenerationKey(WorkoutGenerationKey generationKey) {
		return jpaRepository.existsByGenerationKey(generationKey.value());
	}

	@Override
	public boolean existsByWorkoutDayIdAndOrigin(WorkoutDayId workoutDayId, WorkoutOccurrenceOrigin origin) {
		return jpaRepository.existsByWorkoutDayIdAndOrigin(workoutDayId.value(), origin);
	}

	@Override
	public List<WorkoutOccurrence> findCalendarRange(
			AthleteId athleteId,
			LocalDate from,
			LocalDate to,
			WorkoutOccurrenceStatus status,
			TrainingPlanId trainingPlanId) {
		return jpaRepository
				.findCalendarRange(
						athleteId.value(),
						from,
						to,
						status,
						trainingPlanId == null ? null : trainingPlanId.value())
				.stream()
				.map(WorkoutOccurrencePersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public void delete(WorkoutOccurrence occurrence) {
		jpaRepository.deleteById(occurrence.id().value());
	}

	private static RuntimeException mapConstraint(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
		String lower = message == null ? "" : message.toLowerCase();
		if (lower.contains("uq_workout_occurrences_generation_key")) {
			return new WorkoutOccurrenceGenerationConflictException();
		}
		if (lower.contains("uq_workout_occurrences_day_athlete_date")) {
			return new DuplicateWorkoutOccurrenceException();
		}
		return ex;
	}

}
