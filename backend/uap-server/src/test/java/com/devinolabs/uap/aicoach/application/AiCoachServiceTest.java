package com.devinolabs.uap.aicoach.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import tools.jackson.databind.json.JsonMapper;

import com.devinolabs.uap.aicoach.gateway.AiProviderException;
import com.devinolabs.uap.aicoach.gateway.ModelCompletionResponse;
import com.devinolabs.uap.aicoach.gateway.ModelGateway;
import com.devinolabs.uap.aicoach.infrastructure.config.AiCoachProperties;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachConversationEntity;
import com.devinolabs.uap.athlete.api.AthleteCoachProfileView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView;
import com.devinolabs.uap.training.api.coachcontext.TrainingCoachContextView.Fact;

@ExtendWith(MockitoExtension.class)
class AiCoachServiceTest {

	@Mock
	private CoachContextBuilder contextBuilder;
	@Mock
	private CoachConversationStore conversations;
	@Mock
	private ModelGateway gateway;
	@Mock
	private CoachRateLimiter rateLimiter;

	private AiCoachProperties properties;
	private AiCoachService service;
	private final UUID accountId = UUID.randomUUID();
	private final UUID conversationId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		properties = new AiCoachProperties();
		properties.setEnabled(true);
		service = new AiCoachService(
				properties,
				contextBuilder,
				conversations,
				gateway,
				rateLimiter,
				JsonMapper.builder().build());
	}

	@Test
	void summaryDoesNotCallTheModel() {
		when(contextBuilder.build(accountId)).thenReturn(context(false));
		service.summary(accountId);
		verify(gateway, never()).complete(any());
	}

	@Test
	void refusesAForecastWithoutCallingTheModelWhenContextIsThin() {
		when(conversations.require(accountId, conversationId)).thenReturn(conversation());
		when(contextBuilder.build(accountId)).thenReturn(context(false));
		when(conversations.append(eq(accountId), eq(conversationId), eq("outlook tomorrow"), any(), any()))
				.thenReturn(reply());
		service.reply(accountId, conversationId, "outlook tomorrow");
		verify(gateway, never()).complete(any());
		ArgumentCaptor<CoachOutputGuard.GuardedReply> guarded = ArgumentCaptor.forClass(CoachOutputGuard.GuardedReply.class);
		verify(conversations).append(eq(accountId), eq(conversationId), eq("outlook tomorrow"), guarded.capture(), any());
		assertEquals("REFUSAL", guarded.getValue().contentKind());
	}

	@Test
	void doesNotPersistWhenTheProviderIsDown() {
		when(conversations.require(accountId, conversationId)).thenReturn(conversation());
		when(contextBuilder.build(accountId)).thenReturn(context(true));
		when(conversations.recentTurns(any())).thenReturn(List.of());
		when(gateway.complete(any())).thenThrow(new AiProviderException(AiProviderException.Kind.TIMEOUT));
		assertThrows(AiUnavailableException.class, () -> service.reply(accountId, conversationId, "How am I looking today?"));
		verify(conversations, never()).append(any(), any(), any(), any(), any());
	}

	@Test
	void labelsAGroundedForecastAndDoesNotTreatItAsReadiness() {
		when(conversations.require(accountId, conversationId)).thenReturn(conversation());
		when(contextBuilder.build(accountId)).thenReturn(context(true));
		when(conversations.recentTurns(any())).thenReturn(List.of());
		when(gateway.complete(any())).thenReturn(new ModelCompletionResponse("""
				{"explanation":"UAP already stored a recommendation.","refused":false,
				"forecast":{"horizon":"TOMORROW","outlook":"Keep the next day easy.","uncertainty":"Limited data.","contributingSignals":["recommendation"]}}
				"""));
		when(conversations.append(any(), any(), any(), any(), any())).thenReturn(reply());
		service.reply(accountId, conversationId, "What is the outlook tomorrow?");
		ArgumentCaptor<CoachOutputGuard.GuardedReply> guarded = ArgumentCaptor.forClass(CoachOutputGuard.GuardedReply.class);
		verify(conversations).append(eq(accountId), eq(conversationId), eq("What is the outlook tomorrow?"), guarded.capture(), any());
		assertEquals("FORECAST", guarded.getValue().contentKind());
		assertEquals(true, guarded.getValue().text().startsWith("AI forecast"));
	}

	@Test
	void disabledCoachDoesNotCallTheModelOrSaveATurn() {
		properties.setEnabled(false);
		assertThrows(AiUnavailableException.class, () -> service.reply(accountId, conversationId, "How am I looking today?"));
		verify(gateway, never()).complete(any());
		verify(conversations).require(accountId, conversationId);
		verify(conversations, never()).append(any(), any(), any(), any(), any());
	}

	private CoachConversationEntity conversation() {
		return CoachConversationEntity.start(conversationId, UUID.randomUUID(), accountId, Instant.parse("2026-10-08T12:00:00Z"));
	}

	private static CoachReply reply() {
		return new CoachReply(UUID.randomUUID(), UUID.randomUUID(), "EXPLANATION", "ok", false, null, List.of(), List.of());
	}

	private static CoachContext context(boolean sufficient) {
		Instant now = Instant.parse("2026-10-08T12:00:00Z");
		return new CoachContext(
				UUID.randomUUID(),
				UUID.randomUUID(),
				now,
				LocalDate.of(2026, 10, 8),
				new AthleteCoachProfileView(UUID.randomUUID(), "Ada", "ACTIVE", now, List.of()),
				new TrainingCoachContextView(
						LocalDate.of(2026, 10, 8),
						now,
						Fact.unavailable(),
						Fact.unavailable(),
						Fact.unavailable(),
						Fact.unavailable(),
						Fact.unavailable(),
						List.of()),
				List.of(),
				sufficient,
				List.of("Readiness | UNAVAILABLE | FRESH | No current deterministic readiness assessment is stored."));
	}

}
