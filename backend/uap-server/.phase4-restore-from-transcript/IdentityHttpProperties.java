package com.devinolabs.uap.identity.infrastructure.http;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

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

		private List<String> allowedOrigins = new ArrayList<>(List.of("http://localhost:3000"));
		private List<String> allowedMethods = new ArrayList<>(List.of("GET", "POST", "OPTIONS"));
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
