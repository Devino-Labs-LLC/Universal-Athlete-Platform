package com.devinolabs.uap.identity.infrastructure.security;

import java.io.IOException;
import java.time.Instant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;

final class IdentitySecurityProblemHandlers {

	private IdentitySecurityProblemHandlers() {
	}

	static AuthenticationEntryPoint authenticationEntryPoint() {
		return (HttpServletRequest request, HttpServletResponse response, AuthenticationException ex) ->
				write(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentication is required", request.getRequestURI());
	}

	static AccessDeniedHandler accessDeniedHandler() {
		return (HttpServletRequest request, HttpServletResponse response, AccessDeniedException ex) -> {
			if (ex instanceof CsrfException) {
				write(response, HttpStatus.FORBIDDEN, "CSRF_INVALID", "CSRF token is missing or invalid", request.getRequestURI());
				return;
			}
			write(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access is denied", request.getRequestURI());
		};
	}

	private static void write(
			HttpServletResponse response,
			HttpStatus status,
			String code,
			String message,
			String path) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("""
				{"code":"%s","message":"%s","timestamp":"%s","path":"%s","details":[]}
				""".formatted(code, message, Instant.now(), path).trim());
	}

}
