package com.devinolabs.uap.training.infrastructure.web;

import java.util.List;

public record UpdateDailyRecoveryCheckInRequest(
		PatchValue<Integer> sleepDurationMinutes,
		PatchValue<Integer> sleepQuality,
		PatchValue<Integer> fatigue,
		PatchValue<Integer> muscleSoreness,
		PatchValue<Integer> stress,
		PatchValue<Integer> mood,
		PatchValue<Integer> motivation,
		PatchValue<List<BodyAreaDiscomfortRequest>> discomfortAreas,
		PatchValue<String> notes,
		Long expectedVersion) {
}
