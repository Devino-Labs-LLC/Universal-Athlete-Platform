package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;

interface DailyReadinessAssessmentJpaRepository extends JpaRepository<DailyReadinessAssessmentJpaEntity, UUID> {

	Optional<DailyReadinessAssessmentJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	Optional<DailyReadinessAssessmentJpaEntity> findByDailyAthleteStateSnapshotIdAndAlgorithmVersionAndAthleteId(
			UUID dailyAthleteStateSnapshotId,
			ReadinessAlgorithmVersion algorithmVersion,
			UUID athleteId);

	@Query("""
			select d.dimensionType from DailyReadinessLimitingDimensionJpaEntity d
			join d.assessment a
			where a.id = :assessmentId
			  and a.athleteId = :athleteId
			order by d.rankOrder asc
			""")
	List<ReadinessDimensionType> findLimitingDimensionTypesByAssessmentId(
			@Param("assessmentId") UUID assessmentId,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select a from DailyReadinessAssessmentJpaEntity a
			where a.athleteId = :athleteId
			  and a.stateDate between :startDate and :endDate
			  and (:algorithmVersion is null or a.algorithmVersion = :algorithmVersion)
			  and (
			    :currentSnapshotOnly = false
			    or exists (
			      select 1 from DailyAthleteStateSnapshotJpaEntity s
			      where s.id = a.dailyAthleteStateSnapshotId
			        and s.currentSnapshot = true
			    )
			  )
			order by a.stateDate desc, a.assessedAt desc, a.id asc
			""")
	List<DailyReadinessAssessmentJpaEntity> findHistory(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("currentSnapshotOnly") boolean currentSnapshotOnly,
			@Param("algorithmVersion") ReadinessAlgorithmVersion algorithmVersion,
			Pageable pageable);

	@Query("""
			select count(a) from DailyReadinessAssessmentJpaEntity a
			where a.athleteId = :athleteId
			  and a.stateDate between :startDate and :endDate
			  and (:algorithmVersion is null or a.algorithmVersion = :algorithmVersion)
			  and (
			    :currentSnapshotOnly = false
			    or exists (
			      select 1 from DailyAthleteStateSnapshotJpaEntity s
			      where s.id = a.dailyAthleteStateSnapshotId
			        and s.currentSnapshot = true
			    )
			  )
			""")
	long countHistory(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("currentSnapshotOnly") boolean currentSnapshotOnly,
			@Param("algorithmVersion") ReadinessAlgorithmVersion algorithmVersion);

}
