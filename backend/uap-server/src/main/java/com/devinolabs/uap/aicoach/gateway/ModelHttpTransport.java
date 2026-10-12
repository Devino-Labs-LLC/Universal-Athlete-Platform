package com.devinolabs.uap.aicoach.gateway;

import java.time.Duration;

public interface ModelHttpTransport {

	ModelHttpResult postJson(String url, String bearerToken, String jsonBody, Duration timeout);

	record ModelHttpResult(int statusCode, String body) {
	}

}
