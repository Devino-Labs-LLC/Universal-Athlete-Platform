package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;

public interface DailyReadinessAssessmentRepository {

	DailyReadinessAssessment saveNew(DailyReadinessAssessment assessment);

	Optional<DailyReadinessAssessment> findByIdAndAthleteId(DailyReadinessAssessmentId id, AthleteId athleteId);

	Optional<DailyReadinessAssessment> findBySnapshotIdAndAlgorithmVersion(
			DailyAthleteStateSnapshotId snapshotId,
			ReadinessAlgorithmVersion algorithmVersion,
			AthleteId athleteId);

	/**
	 * Header-only readiness lookup for client facades (no contribution/dimension child hydration).
	 */
	Optional<DailyReadinessAssessmentSummary> findSummaryBySnapshotIdAndAlgorithmVersion(
			DailyAthleteStateSnapshotId snapshotId,
			ReadinessAlgorithmVersion algorithmVersion,
			AthleteId athleteId);

	/**
	 * Scalar limiting-dimension types for client facades — no contribution/strongest hydration.
	 */
	List<ReadinessDimensionType> findLimitingDimensionsByAssessmentId(
			DailyReadinessAssessmentId assessmentId,
			AthleteId athleteId);

	List<DailyReadinessAssessmentSummary> findHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			ReadinessAlgorithmVersion algorithmVersion,
			int page,
			int size);

	/**
	 * Current-snapshot readiness slices for the date. Header fields only.
	 */
	List<CurrentReadinessSlice> findCurrentSlicesByAthleteIdsAndDate(
			Collection<UUID> athleteIds,
			LocalDate stateDate,
			ReadinessAlgorithmVersion algorithmVersion);

	/**
	 * Athlete/dimension pairs for the given assessments. Caller dedupes.
	 */
	List<StoredLimitingDimension> findLimitingDimensionsByAssessmentIds(Collection<UUID> assessmentIds);

	long countHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			ReadinessAlgorithmVersion algorithmVersion);

}
