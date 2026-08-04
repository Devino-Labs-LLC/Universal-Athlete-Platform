package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

interface DailyAthleteStateSnapshotJpaRepository extends JpaRepository<DailyAthleteStateSnapshotJpaEntity, UUID> {

	Optional<DailyAthleteStateSnapshotJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	Optional<DailyAthleteStateSnapshotJpaEntity> findByAthleteIdAndStateDateAndCurrentSnapshotTrue(
			UUID athleteId,
			LocalDate stateDate);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("""
			select s from DailyAthleteStateSnapshotJpaEntity s
			where s.athleteId = :athleteId
			  and s.stateDate = :stateDate
			  and s.currentSnapshot = true
			""")
	Optional<DailyAthleteStateSnapshotJpaEntity> findCurrentForUpdate(
			@Param("athleteId") UUID athleteId,
			@Param("stateDate") LocalDate stateDate);

	@Query("""
			select coalesce(max(s.snapshotVersion), 0) from DailyAthleteStateSnapshotJpaEntity s
			where s.athleteId = :athleteId and s.stateDate = :stateDate
			""")
	int findMaxSnapshotVersion(@Param("athleteId") UUID athleteId, @Param("stateDate") LocalDate stateDate);

	List<DailyAthleteStateSnapshotJpaEntity> findByAthleteIdAndStateDateOrderBySnapshotVersionDesc(
			UUID athleteId,
			LocalDate stateDate);

	@Query("""
			select s from DailyAthleteStateSnapshotJpaEntity s
			where s.athleteId = :athleteId
			  and s.stateDate between :startDate and :endDate
			  and (:currentOnly = false or s.currentSnapshot = true)
			order by s.stateDate desc, s.snapshotVersion desc
			""")
	List<DailyAthleteStateSnapshotJpaEntity> findHistory(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("currentOnly") boolean currentOnly,
			Pageable pageable);

	@Query("""
			select count(s) from DailyAthleteStateSnapshotJpaEntity s
			where s.athleteId = :athleteId
			  and s.stateDate between :startDate and :endDate
			  and (:currentOnly = false or s.currentSnapshot = true)
			""")
	long countHistory(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("currentOnly") boolean currentOnly);

}
