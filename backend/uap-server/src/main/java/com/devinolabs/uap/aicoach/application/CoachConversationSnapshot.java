package com.devinolabs.uap.aicoach.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoachConversationSnapshot(
		UUID id,
		Instant updatedAt,
		List<CoachStoredMessage> messages) {

	public record CoachStoredMessage(
			UUID id,
			String role,
			String contentKind,
			String text,
			boolean forecast,
			String uncertainty,
			Instant createdAt) {
	}

}
