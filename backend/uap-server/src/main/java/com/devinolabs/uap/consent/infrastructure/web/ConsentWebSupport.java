package com.devinolabs.uap.consent.infrastructure.web;

import java.util.UUID;

import org.springframework.security.core.Authentication;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;

final class ConsentWebSupport {

	private ConsentWebSupport() {
	}

	static UUID accountId(Authentication authentication) {
		return requirePrincipal(authentication).accountUuid();
	}

	static AccountPrincipal requirePrincipal(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal;
	}

}
