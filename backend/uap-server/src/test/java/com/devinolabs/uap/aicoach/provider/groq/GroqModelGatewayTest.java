package com.devinolabs.uap.aicoach.provider.groq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

import com.devinolabs.uap.aicoach.gateway.AiProviderException;
import com.devinolabs.uap.aicoach.gateway.ModelCompletionRequest;
import com.devinolabs.uap.aicoach.gateway.ModelHttpTransport;
import com.devinolabs.uap.aicoach.gateway.ModelTransportException;
import com.devinolabs.uap.aicoach.infrastructure.config.AiCoachProperties;

class GroqModelGatewayTest {

	@Test
	void readsContentFromAMockedCompletion() {
		RecordingTransport transport = new RecordingTransport(200, """
				{"choices":[{"message":{"content":"{\\"explanation\\":\\"ok\\"}"}}]}
				""");
		GroqModelGateway gateway = gateway(transport);
		String content = gateway.complete(request()).content();
		assertTrue(content.contains("explanation"));
		assertTrue(transport.body.contains("\"model\":\"llama-test\""));
		assertTrue(transport.url.endsWith("/chat/completions"));
		assertEquals("test-key", transport.token);
	}

	@Test
	void mapsTimeoutAndOutageAndMalformedBodies() {
		GroqModelGateway timeout = gateway(new ThrowingTransport(new ModelTransportException(ModelTransportException.Kind.TIMEOUT)));
		assertEquals(AiProviderException.Kind.TIMEOUT, assertThrows(AiProviderException.class, () -> timeout.complete(request())).kind());

		GroqModelGateway outage = gateway(new RecordingTransport(503, ""));
		assertEquals(AiProviderException.Kind.UNAVAILABLE, assertThrows(AiProviderException.class, () -> outage.complete(request())).kind());

		GroqModelGateway malformed = gateway(new RecordingTransport(200, "{\"choices\":[]}"));
		assertEquals(AiProviderException.Kind.MALFORMED, assertThrows(AiProviderException.class, () -> malformed.complete(request())).kind());
	}

	private static GroqModelGateway gateway(ModelHttpTransport transport) {
		AiCoachProperties properties = new AiCoachProperties();
		properties.setEnabled(true);
		properties.getGroq().setApiKey("test-key");
		properties.getGroq().setModel("llama-test");
		properties.getGroq().setBaseUrl("https://api.groq.com/openai/v1");
		return new GroqModelGateway(properties, transport, JsonMapper.builder().build());
	}

	private static ModelCompletionRequest request() {
		return new ModelCompletionRequest("system", List.of(), "hello", 100);
	}

	private static final class RecordingTransport implements ModelHttpTransport {
		private final int status;
		private final String response;
		private String url;
		private String token;
		private String body;

		private RecordingTransport(int status, String response) {
			this.status = status;
			this.response = response;
		}

		@Override
		public ModelHttpResult postJson(String url, String bearerToken, String jsonBody, Duration timeout) {
			this.url = url;
			this.token = bearerToken;
			this.body = jsonBody;
			return new ModelHttpResult(status, response);
		}
	}

	private static final class ThrowingTransport implements ModelHttpTransport {
		private final ModelTransportException exception;

		private ThrowingTransport(ModelTransportException exception) {
			this.exception = exception;
		}

		@Override
		public ModelHttpResult postJson(String url, String bearerToken, String jsonBody, Duration timeout) {
			throw exception;
		}
	}

}
