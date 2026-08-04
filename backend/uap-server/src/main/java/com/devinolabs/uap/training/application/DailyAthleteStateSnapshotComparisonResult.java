package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotComparisonService;

public record DailyAthleteStateSnapshotComparisonResult(
		UUID olderSnapshotId,
		UUID newerSnapshotId,
		LocalDate olderStateDate,
		LocalDate newerStateDate,
		int olderVersion,
		int newerVersion,
		boolean recoveryChanged,
		boolean baselineChanged,
		boolean trainingLoadChanged,
		boolean scheduleChanged,
		boolean discomfortChanged,
		List<DailyAthleteStateFieldDifferenceResult> fieldDifferences) {

	public static DailyAthleteStateSnapshotComparisonResult from(
			DailyAthleteStateSnapshotComparisonService.DailyAthleteStateSnapshotComparison comparison) {
		return new DailyAthleteStateSnapshotComparisonResult(
				comparison.olderSnapshotId().value(),
				comparison.newerSnapshotId().value(),
				comparison.olderStateDate(),
				comparison.newerStateDate(),
				comparison.olderVersion(),
				comparison.newerVersion(),
				comparison.recoveryChanged(),
				comparison.baselineChanged(),
				comparison.trainingLoadChanged(),
				comparison.scheduleChanged(),
				comparison.discomfortChanged(),
				comparison.fieldDifferences().stream()
						.map(diff -> new DailyAthleteStateFieldDifferenceResult(
								diff.field(), diff.previousValue(), diff.newValue()))
						.toList());
	}

}
