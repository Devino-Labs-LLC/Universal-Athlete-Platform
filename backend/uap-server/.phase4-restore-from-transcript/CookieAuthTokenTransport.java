package com.devinolabs.uap.identity.infrastructure.http;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import com.devinolabs.uap.identity.application.AuthenticationResult;
import com.devinolabs.uap.identity.infrastructure.IdentityAuthProperties;

public class CookieAuthTokenTransport implements AuthTokenTransport {

	private final IdentityHttpProperties httpProperties;
	private final IdentityAuthProperties authProperties;

	public CookieAuthTokenTransport(IdentityHttpProperties httpProperties, IdentityAuthProperties authProperties) {
		this.httpProperties = Objects.requireNonNull(httpProperties);
		this.authProperties = Objects.requireNonNull(authProperties);
	}

	@Override
	public Optional<String> readAccessToken(HttpServletRequest request) {
		return readCookie(request, httpProperties.getCookies().getAccessCookieName());
	}

	@Override
	public Optional<String> readRefreshToken(HttpServletRequest request) {
		return readCookie(request, httpProperties.getCookies().getRefreshCookieName());
	}

	@Override
	public void writeTokens(HttpServletResponse response, AuthenticationResult authenticationResult) {
		Objects.requireNonNull(authenticationResult, "authenticationResult must not be null");
		addCookie(response, buildAccessCookie(authenticationResult.accessToken().token(), authProperties.getAccessTokenTtl()));
		addCookie(response, buildRefreshCookie(authenticationResult.refreshToken(), authProperties.getRefreshTokenTtl()));
	}

	@Override
	public void clearTokens(HttpServletResponse response) {
		addCookie(response, buildAccessCookie("", Duration.ZERO));
		addCookie(response, buildRefreshCookie("", Duration.ZERO));
	}

	private ResponseCookie buildAccessCookie(String value, Duration maxAge) {
		IdentityHttpProperties.Cookies cookies = httpProperties.getCookies();
		return baseCookie(cookies.getAccessCookieName(), value, cookies.getAccessCookiePath(), maxAge);
	}

	private ResponseCookie buildRefreshCookie(String value, Duration maxAge) {
		IdentityHttpProperties.Cookies cookies = httpProperties.getCookies();
		return baseCookie(cookies.getRefreshCookieName(), value, cookies.getRefreshCookiePath(), maxAge);
	}

	private ResponseCookie baseCookie(String name, String value, String path, Duration maxAge) {
		IdentityHttpProperties.Cookies cookies = httpProperties.getCookies();
		ResponseCookie.ResponseCookieBuilder builder = ResponseCookie.from(name, value == null ? "" : value)
				.httpOnly(true)
				.secure(cookies.isSecure())
				.path(path)
				.maxAge(maxAge)
				.sameSite(cookies.getSameSite().name().charAt(0)
						+ cookies.getSameSite().name().substring(1).toLowerCase());
		if (cookies.getDomain() != null && !cookies.getDomain().isBlank()) {
			builder.domain(cookies.getDomain());
		}
		return builder.build();
	}

	private static void addCookie(HttpServletResponse response, ResponseCookie cookie) {
		response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
	}

	private static Optional<String> readCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return Optional.empty();
		}
		return Arrays.stream(cookies)
				.filter(cookie -> name.equals(cookie.getName()))
				.map(Cookie::getValue)
				.filter(value -> value != null && !value.isBlank())
				.findFirst();
	}

}
