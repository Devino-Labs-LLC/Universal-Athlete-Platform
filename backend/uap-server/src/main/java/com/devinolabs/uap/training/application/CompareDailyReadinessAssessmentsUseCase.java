package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.ReadinessDimensionContribution;
import com.devinolabs.uap.training.domain.ReadinessDimensionType;

@Service
public class CompareDailyReadinessAssessmentsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyReadinessAssessmentRepository assessmentRepository;

	public CompareDailyReadinessAssessmentsUseCase(
			AthleteContextPort athleteContextPort,
			DailyReadinessAssessmentRepository assessmentRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
	}

	@Transactional(readOnly = true)
	public DailyReadinessAssessmentComparisonResult execute(
			AccountId accountId,
			UUID olderAssessmentId,
			UUID newerAssessmentId) {
		if (olderAssessmentId == null || newerAssessmentId == null) {
			throw new DailyReadinessCompareInvalidException(
					"olderAssessmentId and newerAssessmentId are required");
		}
		if (olderAssessmentId.equals(newerAssessmentId)) {
			throw new DailyReadinessCompareInvalidException(
					"olderAssessmentId and newerAssessmentId must be different");
		}
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyReadinessAssessment older = assessmentRepository
				.findByIdAndAthleteId(DailyReadinessAssessmentId.of(olderAssessmentId), athleteId)
				.orElseThrow(() -> new DailyReadinessAssessmentNotFoundException(
						"Older readiness assessment not found: " + olderAssessmentId));
		DailyReadinessAssessment newer = assessmentRepository
				.findByIdAndAthleteId(DailyReadinessAssessmentId.of(newerAssessmentId), athleteId)
				.orElseThrow(() -> new DailyReadinessAssessmentNotFoundException(
						"Newer readiness assessment not found: " + newerAssessmentId));

		BigDecimal olderScore = older.scoreValue();
		BigDecimal newerScore = newer.scoreValue();
		BigDecimal scoreDelta = null;
		ScoreDirection scoreDirection = ScoreDirection.UNCHANGED;
		if (olderScore != null && newerScore != null) {
			scoreDelta = newerScore.subtract(olderScore);
			int cmp = scoreDelta.compareTo(BigDecimal.ZERO);
			if (cmp > 0) {
				scoreDirection = ScoreDirection.INCREASED;
			}
			else if (cmp < 0) {
				scoreDirection = ScoreDirection.DECREASED;
			}
		}
		else if (olderScore == null && newerScore != null) {
			scoreDirection = ScoreDirection.INCREASED;
			scoreDelta = newerScore;
		}
		else if (olderScore != null) {
			scoreDirection = ScoreDirection.DECREASED;
			scoreDelta = olderScore.negate();
		}

		List<ReadinessDimensionDifferenceResult> dimensionChanges = compareDimensions(older, newer);
		return new DailyReadinessAssessmentComparisonResult(
				older.id().value(),
				newer.id().value(),
				older.stateDate(),
				newer.stateDate(),
				older.dailyAthleteStateSnapshotId().value(),
				newer.dailyAthleteStateSnapshotId().value(),
				older.dailyAthleteStateSnapshotVersion(),
				newer.dailyAthleteStateSnapshotVersion(),
				olderScore,
				newerScore,
				scoreDelta,
				scoreDirection,
				older.readinessBand() != newer.readinessBand(),
				older.readinessBand(),
				newer.readinessBand(),
				older.dataSufficiency() != newer.dataSufficiency(),
				older.dataSufficiency(),
				newer.dataSufficiency(),
				!older.limitingDimensions().equals(newer.limitingDimensions()),
				older.limitingDimensions(),
				newer.limitingDimensions(),
				dimensionChanges);
	}

	private static List<ReadinessDimensionDifferenceResult> compareDimensions(
			DailyReadinessAssessment older,
			DailyReadinessAssessment newer) {
		Map<ReadinessDimensionType, ReadinessDimensionContribution> olderByType =
				index(older.contributions());
		Map<ReadinessDimensionType, ReadinessDimensionContribution> newerByType =
				index(newer.contributions());
		List<ReadinessDimensionDifferenceResult> diffs = new ArrayList<>();
		for (ReadinessDimensionType type : ReadinessDimensionType.values()) {
			ReadinessDimensionContribution o = olderByType.get(type);
			ReadinessDimensionContribution n = newerByType.get(type);
			if (o == null || n == null) {
				continue;
			}
			if (!Objects.equals(o.normalizedScore(), n.normalizedScore())
					|| o.reasonCode() != n.reasonCode()
					|| o.comparisonBand() != n.comparisonBand()) {
				diffs.add(new ReadinessDimensionDifferenceResult(
						type,
						o.normalizedScore(),
						n.normalizedScore(),
						o.reasonCode(),
						n.reasonCode(),
						o.comparisonBand(),
						n.comparisonBand()));
			}
		}
		return List.copyOf(diffs);
	}

	private static Map<ReadinessDimensionType, ReadinessDimensionContribution> index(
			List<ReadinessDimensionContribution> contributions) {
		Map<ReadinessDimensionType, ReadinessDimensionContribution> map =
				new EnumMap<>(ReadinessDimensionType.class);
		for (ReadinessDimensionContribution contribution : contributions) {
			map.put(contribution.dimensionType(), contribution);
		}
		return map;
	}

	public enum ScoreDirection {
		INCREASED,
		DECREASED,
		UNCHANGED
	}

}
