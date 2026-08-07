package com.devinolabs.uap.training.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Immutable readiness assessment bound to one DailyAthleteStateSnapshot and algorithm version.
 */
public final class DailyReadinessAssessment {

	private final DailyReadinessAssessmentId id;
	private final AthleteId athleteId;
	private final LocalDate stateDate;
	private final DailyAthleteStateSnapshotId dailyAthleteStateSnapshotId;
	private final int dailyAthleteStateSnapshotVersion;
	private final ReadinessAlgorithmVersion algorithmVersion;
	private final ReadinessScore readinessScore;
	private final ReadinessBand readinessBand;
	private final ReadinessDataSufficiency dataSufficiency;
	private final ReadinessReasonCode summaryReasonCode;
	private final int limitingDimensionCount;
	private final int contributingDimensionCount;
	private final Instant assessedAt;
	private final Instant createdAt;
	private final List<ReadinessDimensionContribution> contributions;
	private final List<ReadinessDimensionType> limitingDimensions;
	private final List<ReadinessDimensionType> strongestDimensions;

	private DailyReadinessAssessment(
			DailyReadinessAssessmentId id,
			AthleteId athleteId,
			LocalDate stateDate,
			DailyAthleteStateSnapshotId dailyAthleteStateSnapshotId,
			int dailyAthleteStateSnapshotVersion,
			ReadinessAlgorithmVersion algorithmVersion,
			ReadinessScore readinessScore,
			ReadinessBand readinessBand,
			ReadinessDataSufficiency dataSufficiency,
			ReadinessReasonCode summaryReasonCode,
			int limitingDimensionCount,
			int contributingDimensionCount,
			Instant assessedAt,
			Instant createdAt,
			List<ReadinessDimensionContribution> contributions,
			List<ReadinessDimensionType> limitingDimensions,
			List<ReadinessDimensionType> strongestDimensions) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.stateDate = Objects.requireNonNull(stateDate, "stateDate must not be null");
		this.dailyAthleteStateSnapshotId = Objects.requireNonNull(
				dailyAthleteStateSnapshotId, "dailyAthleteStateSnapshotId must not be null");
		if (dailyAthleteStateSnapshotVersion < 1) {
			throw new IllegalArgumentException("dailyAthleteStateSnapshotVersion must be >= 1");
		}
		this.dailyAthleteStateSnapshotVersion = dailyAthleteStateSnapshotVersion;
		this.algorithmVersion = Objects.requireNonNull(algorithmVersion, "algorithmVersion must not be null");
		this.readinessScore = readinessScore;
		this.readinessBand = Objects.requireNonNull(readinessBand, "readinessBand must not be null");
		this.dataSufficiency = Objects.requireNonNull(dataSufficiency, "dataSufficiency must not be null");
		this.summaryReasonCode = Objects.requireNonNull(summaryReasonCode, "summaryReasonCode must not be null");
		this.limitingDimensionCount = limitingDimensionCount;
		this.contributingDimensionCount = contributingDimensionCount;
		this.assessedAt = Objects.requireNonNull(assessedAt, "assessedAt must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
		this.contributions = List.copyOf(contributions);
		this.limitingDimensions = List.copyOf(limitingDimensions);
		this.strongestDimensions = List.copyOf(strongestDimensions);
	}

	public static DailyReadinessAssessment create(
			DailyAthleteStateSnapshot snapshot,
			ReadinessCalculator.CalculationResult calculation) {
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		Objects.requireNonNull(calculation, "calculation must not be null");
		int contributing = (int) calculation.contributions().stream()
				.filter(c -> c.normalizedScore() != null)
				.count();
		Instant createdAt = calculation.assessedAt();
		return new DailyReadinessAssessment(
				DailyReadinessAssessmentId.generate(),
				snapshot.athleteId(),
				snapshot.stateDate(),
				snapshot.id(),
				snapshot.snapshotVersion(),
				ReadinessCalculator.ALGORITHM_VERSION,
				calculation.readinessScore(),
				calculation.readinessBand(),
				calculation.dataSufficiency(),
				calculation.summaryReasonCode(),
				calculation.limitingDimensions().size(),
				contributing,
				calculation.assessedAt(),
				createdAt,
				calculation.contributions(),
				calculation.limitingDimensions(),
				calculation.strongestDimensions());
	}

	public static DailyReadinessAssessment rehydrate(
			DailyReadinessAssessmentId id,
			AthleteId athleteId,
			LocalDate stateDate,
			DailyAthleteStateSnapshotId dailyAthleteStateSnapshotId,
			int dailyAthleteStateSnapshotVersion,
			ReadinessAlgorithmVersion algorithmVersion,
			ReadinessScore readinessScore,
			ReadinessBand readinessBand,
			ReadinessDataSufficiency dataSufficiency,
			ReadinessReasonCode summaryReasonCode,
			int limitingDimensionCount,
			int contributingDimensionCount,
			Instant assessedAt,
			Instant createdAt,
			List<ReadinessDimensionContribution> contributions,
			List<ReadinessDimensionType> limitingDimensions,
			List<ReadinessDimensionType> strongestDimensions) {
		return new DailyReadinessAssessment(
				id,
				athleteId,
				stateDate,
				dailyAthleteStateSnapshotId,
				dailyAthleteStateSnapshotVersion,
				algorithmVersion,
				readinessScore,
				readinessBand,
				dataSufficiency,
				summaryReasonCode,
				limitingDimensionCount,
				contributingDimensionCount,
				assessedAt,
				createdAt,
				contributions,
				limitingDimensions,
				strongestDimensions);
	}

	public BigDecimal scoreValue() {
		return readinessScore == null ? null : readinessScore.value();
	}

	public DailyReadinessAssessmentId id() {
		return id;
	}

	public AthleteId athleteId() {
		return athleteId;
	}

	public LocalDate stateDate() {
		return stateDate;
	}

	public DailyAthleteStateSnapshotId dailyAthleteStateSnapshotId() {
		return dailyAthleteStateSnapshotId;
	}

	public int dailyAthleteStateSnapshotVersion() {
		return dailyAthleteStateSnapshotVersion;
	}

	public ReadinessAlgorithmVersion algorithmVersion() {
		return algorithmVersion;
	}

	public ReadinessScore readinessScore() {
		return readinessScore;
	}

	public ReadinessBand readinessBand() {
		return readinessBand;
	}

	public ReadinessDataSufficiency dataSufficiency() {
		return dataSufficiency;
	}

	public ReadinessReasonCode summaryReasonCode() {
		return summaryReasonCode;
	}

	public int limitingDimensionCount() {
		return limitingDimensionCount;
	}

	public int contributingDimensionCount() {
		return contributingDimensionCount;
	}

	public Instant assessedAt() {
		return assessedAt;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public List<ReadinessDimensionContribution> contributions() {
		return contributions;
	}

	public List<ReadinessDimensionType> limitingDimensions() {
		return limitingDimensions;
	}

	public List<ReadinessDimensionType> strongestDimensions() {
		return strongestDimensions;
	}

}
