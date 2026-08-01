package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;

interface DailyRecoveryCheckInJpaRepository extends JpaRepository<DailyRecoveryCheckInJpaEntity, UUID> {

	@EntityGraph(DailyRecoveryCheckInJpaEntity.WITH_DISCOMFORT)
	Optional<DailyRecoveryCheckInJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	@EntityGraph(DailyRecoveryCheckInJpaEntity.WITH_DISCOMFORT)
	Optional<DailyRecoveryCheckInJpaEntity> findByAthleteIdAndCheckInDate(UUID athleteId, LocalDate checkInDate);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@EntityGraph(DailyRecoveryCheckInJpaEntity.WITH_DISCOMFORT)
	@Query("SELECT c FROM DailyRecoveryCheckInJpaEntity c WHERE c.id = :id AND c.athleteId = :athleteId")
	Optional<DailyRecoveryCheckInJpaEntity> findByIdAndAthleteIdForUpdate(
			@Param("id") UUID id,
			@Param("athleteId") UUID athleteId);

	boolean existsByAthleteIdAndCheckInDate(UUID athleteId, LocalDate checkInDate);

	@EntityGraph(DailyRecoveryCheckInJpaEntity.WITH_DISCOMFORT)
	@Query("""
			SELECT DISTINCT c FROM DailyRecoveryCheckInJpaEntity c
			LEFT JOIN c.discomfort d
			WHERE c.athleteId = :athleteId
			  AND c.checkInDate BETWEEN :startDate AND :endDate
			  AND (:completeness IS NULL OR c.completeness = :completeness)
			  AND (:minimumFatigue IS NULL OR c.fatigue >= :minimumFatigue)
			  AND (:minimumSoreness IS NULL OR c.muscleSoreness >= :minimumSoreness)
			  AND (:bodyArea IS NULL OR d.bodyArea = :bodyArea)
			ORDER BY c.checkInDate DESC, c.id ASC
			""")
	Page<DailyRecoveryCheckInJpaEntity> findFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("completeness") RecoveryCheckInCompleteness completeness,
			@Param("minimumFatigue") Integer minimumFatigue,
			@Param("minimumSoreness") Integer minimumSoreness,
			@Param("bodyArea") BodyArea bodyArea,
			Pageable pageable);

	@Query("""
			SELECT COUNT(DISTINCT c.id) FROM DailyRecoveryCheckInJpaEntity c
			LEFT JOIN c.discomfort d
			WHERE c.athleteId = :athleteId
			  AND c.checkInDate BETWEEN :startDate AND :endDate
			  AND (:completeness IS NULL OR c.completeness = :completeness)
			  AND (:minimumFatigue IS NULL OR c.fatigue >= :minimumFatigue)
			  AND (:minimumSoreness IS NULL OR c.muscleSoreness >= :minimumSoreness)
			  AND (:bodyArea IS NULL OR d.bodyArea = :bodyArea)
			""")
	long countFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("completeness") RecoveryCheckInCompleteness completeness,
			@Param("minimumFatigue") Integer minimumFatigue,
			@Param("minimumSoreness") Integer minimumSoreness,
			@Param("bodyArea") BodyArea bodyArea);

	@EntityGraph(DailyRecoveryCheckInJpaEntity.WITH_DISCOMFORT)
	List<DailyRecoveryCheckInJpaEntity> findAllByAthleteIdAndCheckInDateBetweenOrderByCheckInDateDescIdAsc(
			UUID athleteId,
			LocalDate startDate,
			LocalDate endDate);

}
