package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.WorkoutOccurrenceLoadSummaryRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceLoadSummary;

@Repository
class JpaWorkoutOccurrenceLoadSummaryRepository implements WorkoutOccurrenceLoadSummaryRepository {

	private final WorkoutOccurrenceLoadSummaryJpaRepository jpaRepository;
	private final WorkoutOccurrenceJpaRepository occurrenceJpaRepository;

	JpaWorkoutOccurrenceLoadSummaryRepository(
			WorkoutOccurrenceLoadSummaryJpaRepository jpaRepository,
			WorkoutOccurrenceJpaRepository occurrenceJpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
		this.occurrenceJpaRepository = Objects.requireNonNull(occurrenceJpaRepository);
	}

	@Override
	public WorkoutOccurrenceLoadSummary save(WorkoutOccurrenceLoadSummary summary) {
		try {
			WorkoutOccurrenceLoadSummaryJpaEntity entity = jpaRepository
					.findById(summary.id().value())
					.map(existing -> WorkoutOccurrenceLoadSummaryPersistenceMapper.toEntity(summary, existing))
					.orElseGet(() -> WorkoutOccurrenceLoadSummaryPersistenceMapper.toEntity(summary));
			WorkoutOccurrenceLoadSummaryJpaEntity saved = jpaRepository.save(entity);
			jpaRepository.flush();
			return loadWithChildren(saved.getId());
		}
		catch (ObjectOptimisticLockingFailureException ex) {
			throw ex;
		}
	}

	@Override
	public Optional<WorkoutOccurrenceLoadSummary> findByOccurrenceIdAndAthleteId(
			WorkoutOccurrenceId occurrenceId,
			AthleteId athleteId) {
		return jpaRepository.findByOccurrenceIdAndAthleteIdWithCategories(occurrenceId.value(), athleteId.value())
				.map(entity -> {
					jpaRepository.findByIdWithMovements(entity.getId());
					return WorkoutOccurrenceLoadSummaryPersistenceMapper.toDomain(entity);
				});
	}

	@Override
	public void deleteAllByAthleteId(AthleteId athleteId) {
		jpaRepository.deleteAllByAthleteId(athleteId.value());
	}

	@Override
	public List<com.devinolabs.uap.training.application.CompletedOccurrenceLoadRow> findCompletedOccurrencesChronologically(
			AthleteId athleteId) {
		return occurrenceJpaRepository.findCompletedByAthleteIdOrderByScheduledDate(athleteId.value()).stream()
				.map(entity -> {
					com.devinolabs.uap.training.domain.WorkoutOccurrence occurrence =
							WorkoutOccurrencePersistenceMapper.toDomain(entity);
					return new com.devinolabs.uap.training.application.CompletedOccurrenceLoadRow(
							occurrence,
							com.devinolabs.uap.training.domain.WorkoutOccurrenceId.of(entity.getId()),
							com.devinolabs.uap.training.domain.TrainingPlanId.of(entity.getTrainingPlanId()),
							com.devinolabs.uap.training.domain.WorkoutDayId.of(entity.getWorkoutDayId()),
							entity.getScheduledDate());
				})
				.toList();
	}

	private WorkoutOccurrenceLoadSummary loadWithChildren(java.util.UUID id) {
		WorkoutOccurrenceLoadSummaryJpaEntity loaded = WorkoutOccurrenceLoadSummaryFetchSupport.loadWithChildren(
				jpaRepository, id);
		return WorkoutOccurrenceLoadSummaryPersistenceMapper.toDomain(loaded);
	}

}
