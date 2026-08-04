package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotComparisonResult;

record DailyAthleteStateSnapshotComparisonResponse(
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
		List<DailyAthleteStateFieldDifferenceResponse> fieldDifferences) {

	static DailyAthleteStateSnapshotComparisonResponse from(DailyAthleteStateSnapshotComparisonResult result) {
		return new DailyAthleteStateSnapshotComparisonResponse(
				result.olderSnapshotId(),
				result.newerSnapshotId(),
				result.olderStateDate(),
				result.newerStateDate(),
				result.olderVersion(),
				result.newerVersion(),
				result.recoveryChanged(),
				result.baselineChanged(),
				result.trainingLoadChanged(),
				result.scheduleChanged(),
				result.discomfortChanged(),
				result.fieldDifferences().stream()
						.map(diff -> new DailyAthleteStateFieldDifferenceResponse(
								diff.field(), diff.previousValue(), diff.newValue()))
						.toList());
	}

}

record DailyAthleteStateFieldDifferenceResponse(
		String field,
		String previousValue,
		String newValue) {
}
