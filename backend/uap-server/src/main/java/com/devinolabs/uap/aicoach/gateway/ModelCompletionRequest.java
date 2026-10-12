package com.devinolabs.uap.aicoach.gateway;

import java.util.List;

public record ModelCompletionRequest(
		String systemPrompt,
		List<ModelTurn> history,
		String userMessage,
		int maxOutputTokens) {

	public record ModelTurn(String role, String content) {
	}

}
