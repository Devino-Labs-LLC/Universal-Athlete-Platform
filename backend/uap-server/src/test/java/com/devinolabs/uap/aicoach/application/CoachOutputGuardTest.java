package com.devinolabs.uap.aicoach.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import com.devinolabs.uap.athlete.api.AthleteCoachProfileView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.Fact;

class CoachOutputGuardTest {

	private final CoachOutputGuard guard = new CoachOutputGuard(JsonMapper.builder().build());
	private final CoachContext empty = context(false, List.of("Readiness | UNAVAILABLE | FRESH | No current deterministic readiness assessment is stored."));

	@Test
	void rejectsDiagnosisAndPlanMutation() {
		String raw = "{\"explanation\":\"I diagnose overtraining and I have updated the plan.\",\"refused\":false,\"forecast\":null}";
		CoachOutputGuard.GuardedReply reply = guard.guard(CoachIntent.GENERAL, empty, raw);
		assertEquals("REFUSAL", reply.contentKind());
		assertEquals(CoachOutputGuard.SAFE_REFUSAL, reply.text());
	}

	@Test
	void rejectsAPrescriptionWithoutADiagnosisWord() {
		String raw = "{\"explanation\":\"I prescribe complete rest and a treatment plan of ibuprofen.\",\"refused\":false,\"forecast\":null}";
		CoachOutputGuard.GuardedReply reply = guard.guard(CoachIntent.GENERAL, empty, raw);
		assertEquals("REFUSAL", reply.contentKind());
		assertEquals(CoachOutputGuard.SAFE_REFUSAL, reply.text());
	}

	@Test
	void rejectsPlanMutationWithoutMedicalLanguage() {
		String raw = "{\"explanation\":\"I have updated the plan for today.\",\"refused\":false,\"forecast\":null}";
		CoachOutputGuard.GuardedReply reply = guard.guard(CoachIntent.GENERAL, empty, raw);
		assertEquals("REFUSAL", reply.contentKind());
		assertEquals(CoachOutputGuard.SAFE_REFUSAL, reply.text());
	}

	@Test
	void replacesAnUnsafeRefusalReason() {
		String raw = "{\"explanation\":\"\",\"refused\":true,\"refusalReason\":\"I diagnose a stress fracture.\"}";
		CoachOutputGuard.GuardedReply reply = guard.guard(CoachIntent.GENERAL, empty, raw);
		assertEquals(CoachOutputGuard.SAFE_REFUSAL, reply.text());
	}

	@Test
	void replacesUnsafeForecastFields() {
		CoachContext ready = context(true, List.of("Readiness | PRESENT | FRESH | Deterministic UAP readiness score 61."));
		String uncertainty = """
				{"explanation":"Stored readiness is on file.","refused":false,
				"forecast":{"horizon":"TOMORROW","outlook":"Keep tomorrow easy.","uncertainty":"I diagnose overtraining.","contributingSignals":["readiness"]}}
				""";
		assertEquals(CoachOutputGuard.SAFE_REFUSAL,
				guard.guard(CoachIntent.SHORT_HORIZON, ready, uncertainty).text());
		String signal = """
				{"explanation":"Stored readiness is on file.","refused":false,
				"forecast":{"horizon":"TOMORROW","outlook":"Keep tomorrow easy.","uncertainty":"Limited data.","contributingSignals":["I have updated the plan"]}}
				""";
		CoachOutputGuard.GuardedReply reply = guard.guard(CoachIntent.SHORT_HORIZON, ready, signal);
		assertEquals("REFUSAL", reply.contentKind());
		assertEquals(CoachOutputGuard.SAFE_REFUSAL, reply.text());
		assertTrue(reply.contributingSignals().isEmpty());
	}

	@Test
	void rejectsInventedReadinessWhenNoneIsStored() {
		String raw = "{\"explanation\":\"Your readiness score is 72.\",\"refused\":false,\"forecast\":null}";
		CoachOutputGuard.GuardedReply reply = guard.guard(CoachIntent.READINESS_EXPLANATION, empty, raw);
		assertEquals("REFUSAL", reply.contentKind());
	}

	@Test
	void fallsBackWhenTheModelReplyIsNotJson() {
		CoachOutputGuard.GuardedReply reply = guard.guard(CoachIntent.GENERAL, empty, "not json");
		assertEquals("FALLBACK", reply.contentKind());
		assertTrue(reply.text().contains("deterministic UAP facts"));
	}

	@Test
	void labelsForecastsAndKeepsThemOutOfReadiness() {
		CoachContext ready = context(true, List.of("Readiness | PRESENT | STALE | Deterministic UAP readiness score 61."));
		String raw = """
				{"explanation":"Based on the stored readiness.","refused":false,
				"forecast":{"horizon":"TOMORROW","outlook":"Keep tomorrow easy.","uncertainty":"","contributingSignals":["readiness"]}}
				""";
		CoachOutputGuard.GuardedReply reply = guard.guard(CoachIntent.SHORT_HORIZON, ready, raw);
		assertEquals("FORECAST", reply.contentKind());
		assertTrue(reply.forecast());
		assertTrue(reply.text().startsWith("AI forecast (not a readiness score):"));
		assertTrue(reply.uncertainty().toLowerCase().contains("stale"));
		assertFalse(reply.text().contains("72"));
	}

	@Test
	void dropsUnsolicitedForecasts() {
		String raw = """
				{"explanation":"Today looks steady from the stored facts.","refused":false,
				"forecast":{"horizon":"TOMORROW","outlook":"Ignore the plan.","uncertainty":"low","contributingSignals":[]}}
				""";
		CoachOutputGuard.GuardedReply reply = guard.guard(CoachIntent.TODAY_STATUS, empty, raw);
		assertEquals("EXPLANATION", reply.contentKind());
		assertFalse(reply.forecast());
	}

	private static CoachContext context(boolean sufficient, List<String> facts) {
		Instant now = Instant.parse("2026-10-08T12:00:00Z");
		TrainingCoachContextView training = new TrainingCoachContextView(
				LocalDate.of(2026, 10, 8),
				now,
				Fact.unavailable(),
				Fact.unavailable(),
				Fact.unavailable(),
				Fact.unavailable(),
				Fact.unavailable(),
				List.of());
		return new CoachContext(
				UUID.randomUUID(),
				UUID.randomUUID(),
				now,
				LocalDate.of(2026, 10, 8),
				new AthleteCoachProfileView(UUID.randomUUID(), "Ada", "ACTIVE", now, List.of()),
				training,
				List.of(),
				sufficient,
				facts);
	}

}
