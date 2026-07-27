package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.PersonalRecordType;

interface AthleteExercisePersonalRecordJpaRepository
		extends JpaRepository<AthleteExercisePersonalRecordJpaEntity, UUID> {

	@Query("""
			select r from AthleteExercisePersonalRecordJpaEntity r
			where r.athleteId = :athleteId
			and (:exercisePerformanceKey is null or r.exercisePerformanceKey = :exercisePerformanceKey)
			and (:recordType is null or r.recordType = :recordType)
			order by r.exercisePerformanceKey asc, r.recordType asc, r.normalizedValue desc, r.id asc
			""")
	List<AthleteExercisePersonalRecordJpaEntity> findFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("exercisePerformanceKey") UUID exercisePerformanceKey,
			@Param("recordType") PersonalRecordType recordType);

	@Query("""
			select r from AthleteExercisePersonalRecordJpaEntity r
			where r.athleteId = :athleteId
			and r.exercisePerformanceKey = :exercisePerformanceKey
			order by r.recordType asc, r.recordQualifier asc, r.id asc
			""")
	List<AthleteExercisePersonalRecordJpaEntity> findAllForKey(
			@Param("athleteId") UUID athleteId,
			@Param("exercisePerformanceKey") UUID exercisePerformanceKey);

	@Query("""
			select r from AthleteExercisePersonalRecordJpaEntity r
			where r.athleteId = :athleteId
			and r.achievedAt >= :achievedFrom
			order by r.achievedAt desc, r.recordType asc, r.id asc
			""")
	List<AthleteExercisePersonalRecordJpaEntity> findRecent(
			@Param("athleteId") UUID athleteId,
			@Param("achievedFrom") Instant achievedFrom,
			Pageable pageable);

	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("""
			delete from AthleteExercisePersonalRecordJpaEntity r
			where r.athleteId = :athleteId
			and (:exercisePerformanceKey is null or r.exercisePerformanceKey = :exercisePerformanceKey)
			""")
	void deleteAllForAthlete(
			@Param("athleteId") UUID athleteId,
			@Param("exercisePerformanceKey") UUID exercisePerformanceKey);

}
