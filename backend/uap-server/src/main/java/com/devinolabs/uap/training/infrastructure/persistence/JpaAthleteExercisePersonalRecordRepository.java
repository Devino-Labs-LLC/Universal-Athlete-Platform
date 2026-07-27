package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.AthleteExercisePersonalRecordRepository;
import com.devinolabs.uap.training.domain.AthleteExercisePersonalRecord;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;
import com.devinolabs.uap.training.domain.PersonalRecordType;

@Repository
class JpaAthleteExercisePersonalRecordRepository implements AthleteExercisePersonalRecordRepository {

	private final AthleteExercisePersonalRecordJpaRepository jpaRepository;

	JpaAthleteExercisePersonalRecordRepository(AthleteExercisePersonalRecordJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public AthleteExercisePersonalRecord save(AthleteExercisePersonalRecord record) {
		boolean isNew = !jpaRepository.existsById(record.id().value());
		AthleteExercisePersonalRecordJpaEntity saved = jpaRepository.save(
				AthleteExercisePersonalRecordPersistenceMapper.toEntity(record, isNew));
		// Flush so a rebuild that deletes and re-inserts inside one transaction cannot trip the
		// slot uniqueness index on statement reordering.
		jpaRepository.flush();
		return AthleteExercisePersonalRecordPersistenceMapper.toDomain(saved);
	}

	@Override
	public List<AthleteExercisePersonalRecord> findAllByAthleteIdAndExercisePerformanceKey(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey) {
		return jpaRepository
				.findAllForKey(athleteId.value(), exercisePerformanceKey.value())
				.stream()
				.map(AthleteExercisePersonalRecordPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<AthleteExercisePersonalRecord> findAllByAthleteId(
			AthleteId athleteId,
			ExercisePerformanceKey exercisePerformanceKey,
			PersonalRecordType recordType) {
		return jpaRepository
				.findFiltered(
						athleteId.value(),
						exercisePerformanceKey == null ? null : exercisePerformanceKey.value(),
						recordType)
				.stream()
				.map(AthleteExercisePersonalRecordPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<AthleteExercisePersonalRecord> findRecentByAthleteId(
			AthleteId athleteId,
			Instant achievedFrom,
			int limit) {
		return jpaRepository
				.findRecent(athleteId.value(), achievedFrom, PageRequest.of(0, limit))
				.stream()
				.map(AthleteExercisePersonalRecordPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public void deleteAllByAthleteId(AthleteId athleteId, ExercisePerformanceKey exercisePerformanceKey) {
		jpaRepository.deleteAllForAthlete(
				athleteId.value(),
				exercisePerformanceKey == null ? null : exercisePerformanceKey.value());
	}

}
