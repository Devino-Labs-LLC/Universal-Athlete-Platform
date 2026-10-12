package com.devinolabs.uap.aicoach.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.devinolabs.uap.aicoach.infrastructure.config.AiCoachProperties;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachConversationEntity;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachConversationRepository;
import com.devinolabs.uap.aicoach.infrastructure.persistence.CoachMessageRepository;
import com.devinolabs.uap.athlete.api.AthleteCoachProfilePort;
import com.devinolabs.uap.athlete.api.AthleteCoachProfileView;

@ExtendWith(MockitoExtension.class)
class CoachConversationStoreTest {

	@Mock
	private CoachConversationRepository conversations;
	@Mock
	private CoachMessageRepository messages;
	@Mock
	private AthleteCoachProfilePort profiles;

	private CoachConversationStore store;
	private final UUID accountId = UUID.randomUUID();
	private final UUID otherAccountId = UUID.randomUUID();
	private final UUID athleteId = UUID.randomUUID();
	private final UUID conversationId = UUID.randomUUID();

	@BeforeEach
	void setUp() {
		store = new CoachConversationStore(
				conversations,
				messages,
				profiles,
				new AiCoachProperties(),
				Clock.fixed(Instant.parse("2026-10-08T12:00:00Z"), ZoneOffset.UTC));
	}

	@Test
	void anotherAthleteCannotReadOrDeleteTheConversation() {
		UUID otherAthleteId = UUID.randomUUID();
		when(profiles.profileForAccount(otherAccountId)).thenReturn(profile(otherAthleteId));
		when(conversations.findByIdAndAthleteId(conversationId, otherAthleteId)).thenReturn(Optional.empty());

		assertThrows(CoachConversationNotFoundException.class, () -> store.require(otherAccountId, conversationId));
		assertThrows(CoachConversationNotFoundException.class, () -> store.deleteOne(otherAccountId, conversationId));
		verify(messages, never()).deleteByConversationId(conversationId);
		verify(conversations, never()).delete(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void ownerCanLoadTheConversation() {
		when(profiles.profileForAccount(accountId)).thenReturn(profile(athleteId));
		when(conversations.findByIdAndAthleteId(conversationId, athleteId)).thenReturn(Optional.of(owned()));
		assertEquals(conversationId, store.require(accountId, conversationId).getId());
	}

	private CoachConversationEntity owned() {
		return CoachConversationEntity.start(conversationId, athleteId, accountId, Instant.parse("2026-10-08T12:00:00Z"));
	}

	private static AthleteCoachProfileView profile(UUID athleteId) {
		return new AthleteCoachProfileView(athleteId, "Ada", "ACTIVE", Instant.parse("2026-10-08T12:00:00Z"), java.util.List.of());
	}

}
