package com.devinolabs.uap.training.infrastructure.web;

import com.devinolabs.uap.training.application.WorkoutOccurrenceLoadSummaryResult;

record OccurrenceTrainingLoadHistoryItemResponse(WorkoutOccurrenceLoadSummaryResponse summary) {

	static OccurrenceTrainingLoadHistoryItemResponse from(WorkoutOccurrenceLoadSummaryResult result) {
		return new OccurrenceTrainingLoadHistoryItemResponse(WorkoutOccurrenceLoadSummaryResponse.from(result));
	}

}
