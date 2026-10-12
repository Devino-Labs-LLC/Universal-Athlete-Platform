package com.devinolabs.uap.aicoach.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachMessageRepository extends JpaRepository<CoachMessageEntity, UUID> {

	List<CoachMessageEntity> findByConversationIdAndAthleteIdOrderByCreatedAtAsc(UUID conversationId, UUID athleteId);

	void deleteByConversationId(UUID conversationId);

	void deleteByAthleteId(UUID athleteId);

}
