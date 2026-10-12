package com.devinolabs.uap.aicoach.provider.groq;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;

import com.devinolabs.uap.aicoach.gateway.ModelHttpTransport;
import com.devinolabs.uap.aicoach.gateway.ModelTransportException;

public final class JdkModelHttpTransport implements ModelHttpTransport {

	private final HttpClient client = HttpClient.newBuilder()
			.connectTimeout(Duration.ofSeconds(5))
			.build();

	@Override
	public ModelHttpResult postJson(String url, String bearerToken, String jsonBody, Duration timeout) {
		try {
			HttpRequest request = HttpRequest.newBuilder(java.net.URI.create(url))
					.timeout(timeout)
					.header("Authorization", "Bearer " + bearerToken)
					.header("Content-Type", "application/json")
					.POST(HttpRequest.BodyPublishers.ofString(jsonBody))
					.build();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			return new ModelHttpResult(response.statusCode(), response.body());
		}
		catch (HttpTimeoutException ex) {
			throw new ModelTransportException(ModelTransportException.Kind.TIMEOUT);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new ModelTransportException(ModelTransportException.Kind.IO);
		}
		catch (Exception ex) {
			throw new ModelTransportException(ModelTransportException.Kind.IO);
		}
	}

}
