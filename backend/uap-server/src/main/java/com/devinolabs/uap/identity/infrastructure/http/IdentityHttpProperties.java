package com.devinolabs.uap.identity.infrastructure.http;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uap.identity.http")
public class IdentityHttpProperties {

	private final Cookies cookies = new Cookies();
	private final Csrf csrf = new Csrf();
	private final Cors cors = new Cors();
	private final RateLimit rateLimit = new RateLimit();
	private TokenDelivery tokenDelivery = TokenDelivery.COOKIE;

	public Cookies getCookies() {
		return cookies;
	}

	public Csrf getCsrf() {
		return csrf;
	}

	public Cors getCors() {
		return cors;
	}

	public RateLimit getRateLimit() {
		return rateLimit;
	}

	public TokenDelivery getTokenDelivery() {
		return tokenDelivery;
	}

	public void setTokenDelivery(TokenDelivery tokenDelivery) {
		this.tokenDelivery = tokenDelivery;
	}

	/**
	 * Always-on HTTP auth transport guards. Safe for local/dev profiles.
	 */
	public void validate() {
		if (tokenDelivery == null) {
			throw new IllegalStateException("uap.identity.http.token-delivery must be configured");
		}
		if (cookies.getSameSite() == SameSite.NONE && !cookies.isSecure()) {
			throw new IllegalStateException(
					"uap.identity.http.cookies.same-site=NONE requires uap.identity.http.cookies.secure=true");
		}
		if (cookies.getAccessCookieName() == null || cookies.getAccessCookieName().isBlank()) {
			throw new IllegalStateException("uap.identity.http.cookies.access-cookie-name must not be blank");
		}
		if (cookies.getRefreshCookieName() == null || cookies.getRefreshCookieName().isBlank()) {
			throw new IllegalStateException("uap.identity.http.cookies.refresh-cookie-name must not be blank");
		}
		if (cors.getAllowedOrigins() == null || cors.getAllowedOrigins().isEmpty()) {
			throw new IllegalStateException(
					"uap.identity.http.cors.allowed-origins must be configured via UAP_CORS_ALLOWED_ORIGINS");
		}
		if (cors.isAllowCredentials() && cors.getAllowedOrigins().stream().anyMatch("*"::equals)) {
			throw new IllegalStateException("CORS must not allow wildcard origins when credentials are enabled");
		}
		if (rateLimit.getCapacity() < 1) {
			throw new IllegalStateException("uap.identity.http.rate-limit.capacity must be at least 1");
		}
		if (rateLimit.getWindow() == null || rateLimit.getWindow().isNegative() || rateLimit.getWindow().isZero()) {
			throw new IllegalStateException("uap.identity.http.rate-limit.window must be a positive duration");
		}
	}

	/**
	 * Production-only guards so the process cannot silently ship with local cookie/CORS defaults.
	 */
	public void validateProductionSafety() {
		validate();
		if (!cookies.isSecure()) {
			throw new IllegalStateException(
					"Production requires uap.identity.http.cookies.secure=true (set UAP_COOKIE_SECURE=true)");
		}
		for (String origin : cors.getAllowedOrigins()) {
			if (origin == null || origin.isBlank()) {
				throw new IllegalStateException(
						"Production CORS allowed-origins must not contain blank entries");
			}
			String normalized = origin.strip().toLowerCase(Locale.ROOT);
			if (normalized.contains("localhost") || normalized.contains("127.0.0.1")) {
				throw new IllegalStateException(
						"Production CORS allowed-origins must not include localhost/loopback defaults; "
								+ "set UAP_CORS_ALLOWED_ORIGINS explicitly");
			}
		}
	}

	public enum TokenDelivery {
		COOKIE,
		BEARER
	}

	public enum SameSite {
		LAX,
		STRICT,
		NONE
	}

	public static class Cookies {

		private String accessCookieName = "uap_at";
		private String refreshCookieName = "uap_rt";
		private boolean secure = false;
		private SameSite sameSite = SameSite.LAX;
		private String domain;
		private String accessCookiePath = "/api";
		private String refreshCookiePath = "/api/v1/identity";

		public String getAccessCookieName() {
			return accessCookieName;
		}

		public void setAccessCookieName(String accessCookieName) {
			this.accessCookieName = accessCookieName;
		}

		public String getRefreshCookieName() {
			return refreshCookieName;
		}

		public void setRefreshCookieName(String refreshCookieName) {
			this.refreshCookieName = refreshCookieName;
		}

		public boolean isSecure() {
			return secure;
		}

		public void setSecure(boolean secure) {
			this.secure = secure;
		}

		public SameSite getSameSite() {
			return sameSite;
		}

		public void setSameSite(SameSite sameSite) {
			this.sameSite = sameSite;
		}

		public String getDomain() {
			return domain;
		}

		public void setDomain(String domain) {
			this.domain = domain;
		}

		public String getAccessCookiePath() {
			return accessCookiePath;
		}

		public void setAccessCookiePath(String accessCookiePath) {
			this.accessCookiePath = accessCookiePath;
		}

		public String getRefreshCookiePath() {
			return refreshCookiePath;
		}

		public void setRefreshCookiePath(String refreshCookiePath) {
			this.refreshCookiePath = refreshCookiePath;
		}

	}

	public static class Csrf {

		private String cookieName = "XSRF-TOKEN";
		private String headerName = "X-XSRF-TOKEN";

		public String getCookieName() {
			return cookieName;
		}

		public void setCookieName(String cookieName) {
			this.cookieName = cookieName;
		}

		public String getHeaderName() {
			return headerName;
		}

		public void setHeaderName(String headerName) {
			this.headerName = headerName;
		}

	}

	public static class Cors {

		private List<String> allowedOrigins = new ArrayList<>(
				List.of("http://localhost:3000", "http://127.0.0.1:3000"));
		private List<String> allowedMethods = new ArrayList<>(
				List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		private List<String> allowedHeaders = new ArrayList<>(
				List.of("Authorization", "Content-Type", "X-XSRF-TOKEN", "X-Requested-With"));
		private boolean allowCredentials = true;

		public List<String> getAllowedOrigins() {
			return allowedOrigins;
		}

		public void setAllowedOrigins(List<String> allowedOrigins) {
			this.allowedOrigins = allowedOrigins;
		}

		public List<String> getAllowedMethods() {
			return allowedMethods;
		}

		public void setAllowedMethods(List<String> allowedMethods) {
			this.allowedMethods = allowedMethods;
		}

		public List<String> getAllowedHeaders() {
			return allowedHeaders;
		}

		public void setAllowedHeaders(List<String> allowedHeaders) {
			this.allowedHeaders = allowedHeaders;
		}

		public boolean isAllowCredentials() {
			return allowCredentials;
		}

		public void setAllowCredentials(boolean allowCredentials) {
			this.allowCredentials = allowCredentials;
		}

	}

	public static class RateLimit {

		private int capacity = 30;
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

}
