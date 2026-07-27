package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.AthleteExercisePersonalRecordHistoryRepository;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecordHistory;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.PersonalRecordType;

@Repository
class JpaAthleteExercisePersonalRecordHistoryRepository
		implements AthleteExercisePersonalRecordHistoryRepository {

	private final AthleteExercisePersonalRecordHistoryJpaRepository jpaRepository;

	JpaAthleteExercisePersonalRecordHistoryRepository(
			AthleteExercisePersonalRecordHistoryJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public AthleteExercisePersonalRecordHistory save(AthleteExercisePersonalRecordHistory entry) {
		boolean isNew = !jpaRepository.existsById(entry.id().value());
		AthleteExercisePersonalRecordHistoryJpaEntity saved = jpaRepository.save(
				AthleteExercisePersonalRecordHistoryPersistenceMapper.toEntity(entry, isNew));
		jpaRepository.flush();
		return AthleteExercisePersonalRecordHistoryPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<AthleteExercisePersonalRecordHistory> findCurrentForSlot(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			PersonalRecordType recordType,
			String recordQualifier) {
		return jpaRepository
				.findSingleCurrentForSlot(
						athleteId.value(),
						exercisePerformanceKey.value(),
						recordType,
						recordQualifier == null ? "" : recordQualifier)
				.map(AthleteExercisePersonalRecordHistoryPersistenceMapper::toDomain);
	}

	@Override
	public List<AthleteExercisePersonalRecordHistory> findAllByAthleteIdAndExercisePerformanceKey(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey) {
		return jpaRepository
				.findAllForKey(athleteId.value(), exercisePerformanceKey.value())
				.stream()
				.map(AthleteExercisePersonalRecordHistoryPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public void deleteAllByAthleteId(AthleteId athleteId, ExercisePerformanceKey exercisePerformanceKey) {
		jpaRepository.deleteAllForAthlete(
				athleteId.value(),
				exercisePerformanceKey == null ? null : exercisePerformanceKey.value());
	}

}
