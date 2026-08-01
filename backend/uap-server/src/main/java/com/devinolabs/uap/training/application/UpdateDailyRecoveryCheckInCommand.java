package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;

public record UpdateDailyRecoveryCheckInCommand(
		Integer sleepDurationMinutes,
		boolean sleepDurationMinutesPresent,
		Integer sleepQuality,
		boolean sleepQualityPresent,
		Integer fatigue,
		boolean fatiguePresent,
		Integer muscleSoreness,
		boolean muscleSorenessPresent,
		Integer stress,
		boolean stressPresent,
		Integer mood,
		boolean moodPresent,
		Integer motivation,
		boolean motivationPresent,
		List<BodyAreaDiscomfortObservation.Input> discomfortAreas,
		boolean discomfortAreasPresent,
		String notes,
		boolean notesPresent,
		Long expectedVersion) {
}
