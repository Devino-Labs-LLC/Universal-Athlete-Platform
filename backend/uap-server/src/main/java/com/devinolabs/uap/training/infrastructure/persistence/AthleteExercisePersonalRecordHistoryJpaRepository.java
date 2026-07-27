package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.PersonalRecordType;

interface AthleteExercisePersonalRecordHistoryJpaRepository
		extends JpaRepository<AthleteExercisePersonalRecordHistoryJpaEntity, UUID> {

	@Query("""
			select h from AthleteExercisePersonalRecordHistoryJpaEntity h
			where h.athleteId = :athleteId
			and h.exercisePerformanceKey = :exercisePerformanceKey
			and h.recordType = :recordType
			and coalesce(h.recordQualifier, '') = :recordQualifierKey
			and h.supersededAt is null
			order by h.achievedAt desc, h.id desc
			""")
	List<AthleteExercisePersonalRecordHistoryJpaEntity> findCurrentForSlot(
			@Param("athleteId") UUID athleteId,
			@Param("exercisePerformanceKey") UUID exercisePerformanceKey,
			@Param("recordType") PersonalRecordType recordType,
			@Param("recordQualifierKey") String recordQualifierKey);

	@Query("""
			select h from AthleteExercisePersonalRecordHistoryJpaEntity h
			where h.athleteId = :athleteId
			and h.exercisePerformanceKey = :exercisePerformanceKey
			order by h.achievedAt asc, h.recordType asc, h.id asc
			""")
	List<AthleteExercisePersonalRecordHistoryJpaEntity> findAllForKey(
			@Param("athleteId") UUID athleteId,
			@Param("exercisePerformanceKey") UUID exercisePerformanceKey);

	default Optional<AthleteExercisePersonalRecordHistoryJpaEntity> findSingleCurrentForSlot(
			UUID athleteId,
			UUID exercisePerformanceKey,
			PersonalRecordType recordType,
			String recordQualifierKey) {
		return findCurrentForSlot(athleteId, exercisePerformanceKey, recordType, recordQualifierKey)
				.stream()
				.findFirst();
	}

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			delete from AthleteExercisePersonalRecordHistoryJpaEntity h
			where h.athleteId = :athleteId
			and (:exercisePerformanceKey is null or h.exercisePerformanceKey = :exercisePerformanceKey)
			""")
	void deleteAllForAthlete(
			@Param("athleteId") UUID athleteId,
			@Param("exercisePerformanceKey") UUID exercisePerformanceKey);

}
