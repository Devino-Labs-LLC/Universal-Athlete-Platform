package com.devinolabs.uap.aicoach.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uap.ai")
public class AiCoachProperties {

	private boolean enabled;
	private String provider = "groq";
	private Duration requestTimeout = Duration.ofSeconds(20);
	private int maxOutputTokens = 600;
	private int maxHistoryMessages = 12;
	private int maxStoredMessages = 40;
	private int maxUserMessageCharacters = 2000;
	private final RateLimit rateLimit = new RateLimit();
	private final Groq groq = new Groq();

	public void validate() {
		if (requestTimeout == null || requestTimeout.isZero() || requestTimeout.isNegative()
				|| requestTimeout.compareTo(Duration.ofSeconds(60)) > 0) {
			throw new IllegalStateException("uap.ai.request-timeout must be between 1s and 60s");
		}
		if (maxOutputTokens < 1 || maxOutputTokens > 1200) {
			throw new IllegalStateException("uap.ai.max-output-tokens must be between 1 and 1200");
		}
		if (maxHistoryMessages < 1 || maxHistoryMessages > 24) {
			throw new IllegalStateException("uap.ai.max-history-messages must be between 1 and 24");
		}
		if (maxStoredMessages < maxHistoryMessages || maxStoredMessages > 80) {
			throw new IllegalStateException("uap.ai.max-stored-messages must cover history and stay at or below 80");
		}
		if (maxUserMessageCharacters < 1 || maxUserMessageCharacters > 4000) {
			throw new IllegalStateException("uap.ai.max-user-message-characters must be between 1 and 4000");
		}
		if (rateLimit.capacity < 1) {
			throw new IllegalStateException("uap.ai.rate-limit.capacity must be at least 1");
		}
		if (rateLimit.window == null || rateLimit.window.isZero() || rateLimit.window.isNegative()) {
			throw new IllegalStateException("uap.ai.rate-limit.window must be positive");
		}
		if (!enabled) {
			return;
		}
		if (provider == null || !"groq".equalsIgnoreCase(provider.trim())) {
			throw new IllegalStateException("uap.ai.provider must be groq while AI is enabled");
		}
		if (groq.apiKey == null || groq.apiKey.isBlank()) {
			throw new IllegalStateException("UAP_AI_GROQ_API_KEY is required when UAP_AI_ENABLED=true");
		}
		if (groq.model == null || groq.model.isBlank()) {
			throw new IllegalStateException("UAP_AI_GROQ_MODEL is required when UAP_AI_ENABLED=true");
		}
		if (groq.baseUrl == null || !groq.baseUrl.startsWith("https://")) {
			throw new IllegalStateException("UAP_AI_GROQ_BASE_URL must use https");
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Duration getRequestTimeout() {
		return requestTimeout;
	}

	public void setRequestTimeout(Duration requestTimeout) {
		this.requestTimeout = requestTimeout;
	}

	public int getMaxOutputTokens() {
		return maxOutputTokens;
	}

	public void setMaxOutputTokens(int maxOutputTokens) {
		this.maxOutputTokens = maxOutputTokens;
	}

	public int getMaxHistoryMessages() {
		return maxHistoryMessages;
	}

	public void setMaxHistoryMessages(int maxHistoryMessages) {
		this.maxHistoryMessages = maxHistoryMessages;
	}

	public int getMaxStoredMessages() {
		return maxStoredMessages;
	}

	public void setMaxStoredMessages(int maxStoredMessages) {
		this.maxStoredMessages = maxStoredMessages;
	}

	public int getMaxUserMessageCharacters() {
		return maxUserMessageCharacters;
	}

	public void setMaxUserMessageCharacters(int maxUserMessageCharacters) {
		this.maxUserMessageCharacters = maxUserMessageCharacters;
	}

	public RateLimit getRateLimit() {
		return rateLimit;
	}

	public Groq getGroq() {
		return groq;
	}

	public static class RateLimit {
		private int capacity = 20;
		private Duration window = Duration.ofMinutes(1);

		public int getCapacity() {
			return capacity;
		}

		public void setCapacity(int capacity) {
			this.capacity = capacity;
		}

		public Duration getWindow() {
			return window;
		}

		public void setWindow(Duration window) {
			this.window = window;
		}
	}

	public static class Groq {
		private String apiKey = "";
		private String model = "llama-3.3-70b-versatile";
		private String baseUrl = "https://api.groq.com/openai/v1";

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public String getModel() {
			return model;
		}

		public void setModel(String model) {
			this.model = model;
		}

		public String getBaseUrl() {
			return baseUrl;
		}

		public void setBaseUrl(String baseUrl) {
			this.baseUrl = baseUrl;
		}
	}

}
