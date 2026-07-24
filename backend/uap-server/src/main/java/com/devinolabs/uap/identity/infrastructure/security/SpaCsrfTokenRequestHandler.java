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
