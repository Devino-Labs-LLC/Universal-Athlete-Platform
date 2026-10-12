package com.devinolabs.uap.aicoach.application;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.aicoach.gateway.ModelCompletionRequest;
import com.devinolabs.uap.aicoach.infrastructure.config.AiCoachProperties;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachConversationEntity;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachConversationRepository;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachMessageEntity;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachMessageRepository;

@Service
public class CoachConversationStore {

	private final CoachConversationRepository conversations;
	private final CoachMessageRepository messages;
	private final AthleteCoachProfileReader profiles;
	private final AiCoachProperties properties;
	private final Clock clock;

	public CoachConversationStore(
			CoachConversationRepository conversations,
			CoachMessageRepository messages,
			com.devinolabs.uap.athlete.api.AthleteCoachProfilePort profiles,
			AiCoachProperties properties,
			Clock clock) {
		this.conversations = Objects.requireNonNull(conversations);
		this.messages = Objects.requireNonNull(messages);
		this.profiles = profiles::profileForAccount;
		this.properties = Objects.requireNonNull(properties);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public UUID start(UUID accountId) {
		var profile = profiles.profile(accountId);
		var entity = CoachConversationEntity.start(UUID.randomUUID(), profile.athleteId(), accountId, clock.instant());
		return conversations.save(entity).getId();
	}

	@Transactional(readOnly = true)
	public CoachConversationSnapshot current(UUID accountId) {
		var profile = profiles.profile(accountId);
		return conversations.findFirstByAthleteIdOrderByUpdatedAtDesc(profile.athleteId())
				.filter(conversation -> accountId.equals(conversation.getAccountId()))
				.map(this::snapshot)
				.orElse(null);
	}

	@Transactional
	public void deleteOne(UUID accountId, UUID conversationId) {
		CoachConversationEntity conversation = require(accountId, conversationId);
		messages.deleteByConversationId(conversation.getId());
		conversations.delete(conversation);
	}

	@Transactional
	public void clear(UUID accountId) {
		var profile = profiles.profile(accountId);
		messages.deleteByAthleteId(profile.athleteId());
		conversations.deleteByAthleteId(profile.athleteId());
	}

	@Transactional(readOnly = true)
	public CoachConversationEntity require(UUID accountId, UUID conversationId) {
		var profile = profiles.profile(accountId);
		return conversations.findByIdAndAthleteId(conversationId, profile.athleteId())
				.filter(conversation -> accountId.equals(conversation.getAccountId()))
				.orElseThrow(CoachConversationNotFoundException::new);
	}

	@Transactional(readOnly = true)
	public List<ModelCompletionRequest.ModelTurn> recentTurns(CoachConversationEntity conversation) {
		List<CoachMessageEntity> stored = messages.findByConversationIdAndAthleteIdOrderByCreatedAtAsc(
				conversation.getId(), conversation.getAthleteId());
		List<ModelCompletionRequest.ModelTurn> turns = stored.stream()
				.map(message -> new ModelCompletionRequest.ModelTurn(
						"USER".equals(message.getRole()) ? "user" : "assistant",
						message.getBody()))
				.toList();
		return CoachPromptComposer.boundedHistory(turns, properties.getMaxHistoryMessages());
	}

	@Transactional
	public CoachReply append(
			UUID accountId,
			UUID conversationId,
			String userText,
			CoachOutputGuard.GuardedReply reply,
			List<String> deterministicFacts) {
		CoachConversationEntity conversation = require(accountId, conversationId);
		Instant now = clock.instant();
		messages.save(CoachMessageEntity.create(
				UUID.randomUUID(),
				conversation.getId(),
				conversation.getAthleteId(),
				"USER",
				userText,
				"EXPLANATION",
				false,
				null,
				now));
		CoachMessageEntity assistant = messages.save(CoachMessageEntity.create(
				UUID.randomUUID(),
				conversation.getId(),
				conversation.getAthleteId(),
				"ASSISTANT",
				reply.text(),
				reply.contentKind(),
				reply.forecast(),
				reply.uncertainty(),
				now.plusMillis(1)));
		trim(conversation);
		conversation.touch(now);
		conversations.save(conversation);
		return new CoachReply(
				conversation.getId(),
				assistant.getId(),
				assistant.getContentKind(),
				assistant.getBody(),
				assistant.isForecast(),
				assistant.getUncertainty(),
				reply.contributingSignals(),
				deterministicFacts);
	}

	private void trim(CoachConversationEntity conversation) {
		List<CoachMessageEntity> stored = messages.findByConversationIdAndAthleteIdOrderByCreatedAtAsc(
				conversation.getId(), conversation.getAthleteId());
		int overflow = stored.size() - properties.getMaxStoredMessages();
		if (overflow <= 0) {
			return;
		}
		messages.deleteAll(List.copyOf(stored.subList(0, overflow)));
	}

	private CoachConversationSnapshot snapshot(CoachConversationEntity conversation) {
		List<CoachConversationSnapshot.CoachStoredMessage> stored = messages
				.findByConversationIdAndAthleteIdOrderByCreatedAtAsc(conversation.getId(), conversation.getAthleteId())
				.stream()
				.map(message -> new CoachConversationSnapshot.CoachStoredMessage(
						message.getId(),
						message.getRole(),
						message.getContentKind(),
						message.getBody(),
						message.isForecast(),
						message.getUncertainty(),
						message.getCreatedAt()))
				.toList();
		return new CoachConversationSnapshot(conversation.getId(), conversation.getUpdatedAt(), stored);
	}

	@FunctionalInterface
	interface AthleteCoachProfileReader {
		com.devinolabs.uap.athlete.api.AthleteCoachProfileView profile(UUID accountId);
	}

}
