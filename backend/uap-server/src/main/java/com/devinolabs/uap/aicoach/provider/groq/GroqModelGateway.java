package com.devinolabs.uap.aicoach.provider.groq;

import java.util.Objects;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import com.devinolabs.uap.aicoach.gateway.AiProviderException;
import com.devinolabs.uap.aicoach.gateway.ModelCompletionRequest;
import com.devinolabs.uap.aicoach.gateway.ModelCompletionResponse;
import com.devinolabs.uap.aicoach.gateway.ModelGateway;
import com.devinolabs.uap.aicoach.gateway.ModelHttpTransport;
import com.devinolabs.uap.aicoach.gateway.ModelTransportException;
import com.devinolabs.uap.aicoach.infrastructure.config.AiCoachProperties;

/**
 * Groq chat-completions adapter. The model id and credentials come from configuration.
 */
public final class GroqModelGateway implements ModelGateway {

	private final AiCoachProperties properties;
	private final ModelHttpTransport transport;
	private final JsonMapper objectMapper;

	public GroqModelGateway(
			AiCoachProperties properties,
			ModelHttpTransport transport,
			JsonMapper objectMapper) {
		this.properties = Objects.requireNonNull(properties);
		this.transport = Objects.requireNonNull(transport);
		this.objectMapper = Objects.requireNonNull(objectMapper);
	}

	@Override
	public ModelCompletionResponse complete(ModelCompletionRequest request) {
		Objects.requireNonNull(request, "request must not be null");
		try {
			String url = completionsUrl(properties.getGroq().getBaseUrl());
			ModelHttpTransport.ModelHttpResult result = transport.postJson(
					url,
					properties.getGroq().getApiKey(),
					body(request),
					properties.getRequestTimeout());
			if (result.statusCode() < 200 || result.statusCode() >= 300) {
				throw new AiProviderException(AiProviderException.Kind.UNAVAILABLE);
			}
			return new ModelCompletionResponse(readContent(result.body()));
		}
		catch (ModelTransportException ex) {
			throw new AiProviderException(ex.kind() == ModelTransportException.Kind.TIMEOUT
					? AiProviderException.Kind.TIMEOUT
					: AiProviderException.Kind.UNAVAILABLE);
		}
		catch (AiProviderException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new AiProviderException(AiProviderException.Kind.MALFORMED);
		}
	}

	private String body(ModelCompletionRequest request) {
		ObjectNode root = objectMapper.createObjectNode();
		root.put("model", properties.getGroq().getModel());
		root.put("temperature", 0.2);
		root.put("max_tokens", request.maxOutputTokens());
		root.putObject("response_format").put("type", "json_object");
		ArrayNode messages = root.putArray("messages");
		messages.addObject().put("role", "system").put("content", request.systemPrompt());
		for (ModelCompletionRequest.ModelTurn turn : request.history()) {
			if ("user".equals(turn.role()) || "assistant".equals(turn.role())) {
				messages.addObject().put("role", turn.role()).put("content", turn.content());
			}
		}
		messages.addObject().put("role", "user").put("content", request.userMessage());
		return root.toString();
	}

	private String readContent(String body) {
		try {
			JsonNode content = objectMapper.readTree(body == null ? "" : body)
					.path("choices")
					.path(0)
					.path("message")
					.path("content");
			if (!content.isTextual() || content.asText().isBlank()) {
				throw new AiProviderException(AiProviderException.Kind.MALFORMED);
			}
			return content.asText();
		}
		catch (AiProviderException ex) {
			throw ex;
		}
		catch (Exception ex) {
			throw new AiProviderException(AiProviderException.Kind.MALFORMED);
		}
	}

	static String completionsUrl(String baseUrl) {
		String trimmed = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
		return trimmed + "/chat/completions";
	}

}
