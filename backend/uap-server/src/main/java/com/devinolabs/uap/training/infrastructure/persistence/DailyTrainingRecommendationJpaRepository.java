package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.TrainingAdjustmentType;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;

interface DailyTrainingRecommendationJpaRepository
		extends JpaRepository<DailyTrainingRecommendationJpaEntity, UUID> {

	Optional<DailyTrainingRecommendationJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	Optional<DailyTrainingRecommendationJpaEntity>
			findByDailyReadinessAssessmentIdAndRecommendationAlgorithmVersionAndAthleteId(
					UUID dailyReadinessAssessmentId,
					TrainingRecommendationAlgorithmVersion recommendationAlgorithmVersion,
					UUID athleteId);

	@Query("""
			select a.adjustmentType from DailyTrainingRecommendationAdjustmentJpaEntity a
			join a.recommendation r
			where r.id = :recommendationId
			  and r.athleteId = :athleteId
			order by a.orderIndex asc
			""")
	List<TrainingAdjustmentType> findAdjustmentTypesByRecommendationId(
			@Param("recommendationId") UUID recommendationId,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select r from DailyTrainingRecommendationJpaEntity r
			where r.athleteId = :athleteId
			  and r.stateDate between :startDate and :endDate
			  and (:algorithmVersion is null or r.recommendationAlgorithmVersion = :algorithmVersion)
			  and (:overallAction is null or r.overallAction = :overallAction)
			  and (
			    :currentSnapshotOnly = false
			    or exists (
			      select 1 from DailyAthleteStateSnapshotJpaEntity s
			      where s.id = r.dailyAthleteStateSnapshotId
			        and s.currentSnapshot = true
			    )
			  )
			order by r.stateDate desc, r.generatedAt desc, r.id asc
			""")
	List<DailyTrainingRecommendationJpaEntity> findHistory(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("currentSnapshotOnly") boolean currentSnapshotOnly,
			@Param("algorithmVersion") TrainingRecommendationAlgorithmVersion algorithmVersion,
			@Param("overallAction") TrainingRecommendationAction overallAction,
			Pageable pageable);

	@Query("""
			select count(r) from DailyTrainingRecommendationJpaEntity r
			where r.athleteId = :athleteId
			  and r.stateDate between :startDate and :endDate
			  and (:algorithmVersion is null or r.recommendationAlgorithmVersion = :algorithmVersion)
			  and (:overallAction is null or r.overallAction = :overallAction)
			  and (
			    :currentSnapshotOnly = false
			    or exists (
			      select 1 from DailyAthleteStateSnapshotJpaEntity s
			      where s.id = r.dailyAthleteStateSnapshotId
			        and s.currentSnapshot = true
			    )
			  )
			""")
	long countHistory(
			@Param("athleteId") UUID athleteId,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("currentSnapshotOnly") boolean currentSnapshotOnly,
			@Param("algorithmVersion") TrainingRecommendationAlgorithmVersion algorithmVersion,
			@Param("overallAction") TrainingRecommendationAction overallAction);

}
