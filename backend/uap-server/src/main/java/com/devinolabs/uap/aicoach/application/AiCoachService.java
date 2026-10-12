package com.devinolabs.uap.aicoach.application;

import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import tools.jackson.databind.json.JsonMapper;

import com.devinolabs.uap.aicoach.gateway.AiProviderException;
import com.devinolabs.uap.aicoach.gateway.ModelGateway;
import com.devinolabs.uap.aicoach.infrastructure.config.AiCoachProperties;

@Service
public class AiCoachService {

	private static final Logger log = LoggerFactory.getLogger(AiCoachService.class);
	private static final String INSUFFICIENT_FORECAST = "I can't give a short-horizon outlook because UAP does not have "
			+ "enough current readiness, recovery, training, or connected evidence. I won't invent one.";

	private final AiCoachProperties properties;
	private final CoachContextBuilder contextBuilder;
	private final CoachConversationStore conversations;
	private final ModelGateway gateway;
	private final CoachRateLimiter rateLimiter;
	private final CoachOutputGuard outputGuard;

	public AiCoachService(
			AiCoachProperties properties,
			CoachContextBuilder contextBuilder,
			CoachConversationStore conversations,
			ModelGateway gateway,
			CoachRateLimiter rateLimiter,
			JsonMapper objectMapper) {
		this.properties = Objects.requireNonNull(properties);
		this.contextBuilder = Objects.requireNonNull(contextBuilder);
		this.conversations = Objects.requireNonNull(conversations);
		this.gateway = Objects.requireNonNull(gateway);
		this.rateLimiter = Objects.requireNonNull(rateLimiter);
		this.outputGuard = new CoachOutputGuard(objectMapper);
	}

	public CoachContext summary(UUID accountId) {
		return contextBuilder.build(accountId);
	}

	public UUID startConversation(UUID accountId) {
		return conversations.start(accountId);
	}

	public CoachConversationSnapshot currentConversation(UUID accountId) {
		return conversations.current(accountId);
	}

	public void deleteConversation(UUID accountId, UUID conversationId) {
		conversations.deleteOne(accountId, conversationId);
	}

	public void clearConversations(UUID accountId) {
		conversations.clear(accountId);
	}

	public CoachReply reply(UUID accountId, UUID conversationId, String text) {
		String message = requireMessage(text);
		rateLimiter.acquire(accountId);
		conversations.require(accountId, conversationId);
		if (!properties.isEnabled()) {
			throw new AiUnavailableException();
		}
		CoachContext context = contextBuilder.build(accountId);
		CoachIntent intent = CoachIntent.classify(message);
		CoachOutputGuard.GuardedReply guarded = produce(accountId, conversationId, message, context, intent);
		return conversations.append(accountId, conversationId, message, guarded, context.deterministicFacts());
	}

	private CoachOutputGuard.GuardedReply produce(
			UUID accountId,
			UUID conversationId,
			String message,
			CoachContext context,
			CoachIntent intent) {
		if (intent == CoachIntent.SHORT_HORIZON && !context.sufficientForForecast()) {
			return CoachOutputGuard.GuardedReply.refusal(INSUFFICIENT_FORECAST);
		}
		try {
			var response = gateway.complete(CoachPromptComposer.compose(
					context,
					intent,
					conversations.recentTurns(conversations.require(accountId, conversationId)),
					message,
					properties.getMaxOutputTokens()));
			return outputGuard.guard(intent, context, response.content());
		}
		catch (AiProviderException ex) {
			log.warn("Coach provider failed code={}", ex.kind());
			throw new AiUnavailableException();
		}
	}

	private String requireMessage(String text) {
		if (text == null || text.isBlank() || text.trim().length() > properties.getMaxUserMessageCharacters()) {
			throw new CoachMessageInvalidException();
		}
		return text.trim();
	}

	public boolean aiEnabled() {
		return properties.isEnabled();
	}

}
