package com.devinolabs.uap.identity.infrastructure.http;

import java.io.IOException;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class IdentityRateLimitFilter extends OncePerRequestFilter {

	private static final Set<String> PROTECTED_PATHS = Set.of(
			"/api/v1/identity/register",
			"/api/v1/identity/login",
			"/api/v1/identity/verify-email",
			"/api/v1/identity/refresh");

	private final RateLimiter rateLimiter;

	public IdentityRateLimitFilter(RateLimiter rateLimiter) {
		this.rateLimiter = rateLimiter;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String path = request.getRequestURI();
		if ("POST".equalsIgnoreCase(request.getMethod()) && PROTECTED_PATHS.contains(path)) {
			String key = path + ":" + clientKey(request);
			if (!rateLimiter.tryAcquire(key)) {
				response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
				response.setContentType(MediaType.APPLICATION_JSON_VALUE);
				response.getWriter().write("""
						{"code":"RATE_LIMITED","message":"Too many requests. Please try again later.","path":"%s"}
						""".formatted(path).trim());
				return;
			}
		}
		filterChain.doFilter(request, response);
	}

	private static String clientKey(HttpServletRequest request) {
		String forwarded = request.getHeader("X-Forwarded-For");
		if (forwarded != null && !forwarded.isBlank()) {
			return forwarded.split(",")[0].trim();
		}
		return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
	}

}
