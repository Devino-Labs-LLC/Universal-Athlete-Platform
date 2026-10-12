package com.devinolabs.uap.aicoach.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachConversationRepository extends JpaRepository<CoachConversationEntity, UUID> {

	Optional<CoachConversationEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	Optional<CoachConversationEntity> findFirstByAthleteIdOrderByUpdatedAtDesc(UUID athleteId);

	void deleteByAthleteId(UUID athleteId);

}
