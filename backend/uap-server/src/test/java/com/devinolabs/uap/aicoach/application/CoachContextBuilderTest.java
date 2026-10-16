package com.devinolabs.uap.aicoach.application;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devinolabs.uap.athlete.api.AthleteCoachProfilePort;
import com.devinolabs.uap.athlete.api.AthleteCoachProfileView;
import com.devinolabs.uap.integrations.api.connections.AthleteConnectionStatusPort;
import com.devinolabs.uap.integrations.api.connections.ConnectionView;
import com.devinolabs.uap.integrations.api.evidence.ConnectedEvidencePort;
import com.devinolabs.uap.integrations.api.evidence.ConnectedEvidenceView;
import com.devinolabs.uap.integrations.api.evidence.FusedSensorPort;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextPort;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.Fact;

@ExtendWith(MockitoExtension.class)
class CoachContextBuilderTest {

	@Mock
	private AthleteCoachProfilePort profilePort;
	@Mock
	private TrainingCoachContextPort trainingPort;
	@Mock
	private AthleteConnectionStatusPort connectionPort;
	@Mock
	private ConnectedEvidencePort evidencePort;
	@Mock
	private FusedSensorPort fusedSensorPort;

	@Test
	void withholdsRawEvidenceValuesAndIds() {
		UUID accountId = UUID.randomUUID();
		UUID athleteId = UUID.randomUUID();
		UUID connectionId = UUID.randomUUID();
		Instant now = Instant.parse("2026-10-08T15:00:00Z");
		when(profilePort.profileForAccount(accountId)).thenReturn(
				new AthleteCoachProfileView(athleteId, "Ada", "ACTIVE", now, List.of()));
		when(trainingPort.load(accountId, java.time.LocalDate.of(2026, 10, 8))).thenReturn(emptyTraining(now));
		when(connectionPort.listForAccount(accountId)).thenReturn(List.of(connection(connectionId, now)));
		when(evidencePort.listEvidence(athleteId, now.minus(java.time.Duration.ofDays(7)), now, null))
				.thenReturn(List.of(evidence(connectionId, now)));
		when(fusedSensorPort.listFused(athleteId, now.minus(java.time.Duration.ofDays(7)), now, null))
				.thenReturn(List.of());

		CoachContextBuilder builder = new CoachContextBuilder(
				profilePort,
				trainingPort,
				connectionPort,
				evidencePort,
				fusedSensorPort,
				Clock.fixed(now, ZoneOffset.UTC));
		CoachContext context = builder.build(accountId);
		String rendered = String.join("\n", context.deterministicFacts());

		assertFalse(rendered.contains("12345"));
		assertFalse(rendered.contains("ext-1"));
		assertFalse(rendered.contains(athleteId.toString()));
		assertFalse(rendered.contains(connectionId.toString()));
		assertTrue(rendered.contains("Individual sample values are withheld"));
		assertTrue(rendered.contains("FRESH"));
		assertTrue(context.sufficientForForecast());

		var request = CoachPromptComposer.compose(context, CoachIntent.GENERAL, java.util.List.of(), "How am I looking today?", 100);
		String prompt = request.systemPrompt() + "\n" + request.userMessage();
		assertFalse(prompt.contains("12345"));
		assertFalse(prompt.contains("ext-1"));
		assertFalse(prompt.contains(athleteId.toString()));
		assertFalse(prompt.contains(connectionId.toString()));
		assertFalse(prompt.contains("170"));
		assertFalse(prompt.contains("65"));
	}

	private static TrainingCoachContextView emptyTraining(Instant now) {
		return new TrainingCoachContextView(
				java.time.LocalDate.of(2026, 10, 8),
				now,
				Fact.unavailable(),
				Fact.unavailable(),
				Fact.unavailable(),
				Fact.unavailable(),
				Fact.unavailable(),
				List.of());
	}

	private static ConnectionView connection(UUID connectionId, Instant now) {
		return new ConnectionView(connectionId, "APPLE_HEALTHKIT", "CONNECTED", true, now, now, null, now, now);
	}

	private static ConnectedEvidenceView evidence(UUID connectionId, Instant now) {
		return new ConnectedEvidenceView(
				UUID.randomUUID(),
				connectionId,
				"APPLE_HEALTHKIT",
				"HEART",
				"RESTING_HEART_RATE",
				"ext-1",
				new BigDecimal("12345.5"),
				null,
				"count/min",
				now,
				now,
				now,
				now,
				now,
				null,
				"OS_HUB",
				null,
				null,
				"ACTIVE",
				null);
	}

}
