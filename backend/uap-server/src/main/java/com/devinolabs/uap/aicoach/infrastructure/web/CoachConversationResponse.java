package com.devinolabs.uap.aicoach.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CoachConversationResponse(
		UUID id,
		Instant updatedAt,
		List<Message> messages) {

	public record Message(
			UUID id,
			String role,
			String contentKind,
			String text,
			boolean forecast,
			String uncertainty,
			Instant createdAt) {
	}

}
