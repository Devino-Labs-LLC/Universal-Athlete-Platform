package com.devinolabs.uap.identity.infrastructure.http;

import java.util.Optional;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.devinolabs.uap.identity.application.AuthenticationResult;

public interface AuthTokenTransport {

	Optional<String> readAccessToken(HttpServletRequest request);

	Optional<String> readRefreshToken(HttpServletRequest request);

	void writeTokens(HttpServletResponse response, AuthenticationResult authenticationResult);

	void clearTokens(HttpServletResponse response);

}
