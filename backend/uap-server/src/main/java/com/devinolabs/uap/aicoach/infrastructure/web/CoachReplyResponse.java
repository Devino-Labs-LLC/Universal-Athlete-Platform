package com.devinolabs.uap.aicoach.infrastructure.web;

import java.util.List;
import java.util.UUID;

public record CoachReplyResponse(
		UUID conversationId,
		UUID messageId,
		String contentKind,
		String text,
		boolean forecast,
		String uncertainty,
		List<String> contributingSignals,
		List<String> deterministicFacts) {
}
