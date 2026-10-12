package com.devinolabs.uap.aicoach.application;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.athlete.api.AthleteCoachProfileView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.Fact;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.ReadinessFact;

class CoachContextFactsTest {

	@Test
	void staleReadinessIsLabeledForTheModel() {
		Instant observed = Instant.parse("2026-10-06T12:00:00Z");
		TrainingCoachContextView training = new TrainingCoachContextView(
				LocalDate.of(2026, 10, 8),
				Instant.parse("2026-10-08T15:00:00Z"),
				Fact.present(observed, true, new ReadinessFact("61", "READY", "SUFFICIENT", "RECENT_LOAD")),
				Fact.unavailable(),
				Fact.unavailable(),
				Fact.unavailable(),
				Fact.unavailable(),
				List.of());
		String rendered = String.join("\n", CoachContextFacts.describe(
				new AthleteCoachProfileView(
						java.util.UUID.randomUUID(),
						"Ada",
						"ACTIVE",
						observed,
						List.of()),
				training,
				List.of(new ConnectedSignalSummary(
						"APPLE_HEALTHKIT",
						"CONNECTED",
						"STALE",
						null,
						null,
						0,
						List.of()))));
		assertTrue(rendered.contains("Readiness | PRESENT | STALE |"));
		assertTrue(rendered.contains("Connected evidence | UNAVAILABLE | STALE |"));
		assertTrue(rendered.contains("Freshness STALE"));
	}

}
