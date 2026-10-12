package com.devinolabs.uap.aicoach.application;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CoachReply(
		UUID conversationId,
		UUID messageId,
		String contentKind,
		String text,
		boolean forecast,
		String uncertainty,
		List<String> contributingSignals,
		List<String> deterministicFacts) {
}
