package com.devinolabs.uap.aicoach.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.devinolabs.uap.athlete.api.AthleteCoachProfileView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView;

public record CoachContext(
		UUID athleteId,
		UUID accountId,
		Instant builtAt,
		LocalDate asOf,
		AthleteCoachProfileView profile,
		TrainingCoachContextView training,
		List<ConnectedSignalSummary> signals,
		boolean sufficientForForecast,
		List<String> deterministicFacts) {
}
