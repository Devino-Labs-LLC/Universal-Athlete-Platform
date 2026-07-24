package com.devinolabs.uap.identity.infrastructure.security;

import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * Double-submit CSRF strategy for browser SPAs and cookie-authenticated APIs.
 *
 * <p>Cookie {@code XSRF-TOKEN} (readable by JS) must be echoed in {@code X-XSRF-TOKEN}.
 * Header values use plain comparison so React/web clients can copy the cookie.
 * Form-style XOR tokens remain supported when no header is present.
 *
 * <p>Public register/verify/login ignore CSRF. Authenticated cookie mutations
 * (refresh, logout, logout-all) and other CSRF-protected POSTs require the header.
 * Future React Native clients should prefer bearer delivery ({@code TokenDelivery.BEARER})
 * or send the same CSRF header when using cookies.
 */
final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

	private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
	private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
		xor.handle(request, response, csrfToken);
		csrfToken.get().getToken();
	}

	@Override
	public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
		if (StringUtils.hasText(request.getHeader(csrfToken.getHeaderName()))) {
			return plain.resolveCsrfTokenValue(request, csrfToken);
		}
		return xor.resolveCsrfTokenValue(request, csrfToken);
	}

}
