package com.devinolabs.uap.organization.infrastructure.web;

import java.util.Objects;
import java.util.UUID;

import org.springframework.security.core.Authentication;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.domain.AccountId;

final class OrganizationWebSupport {

	private OrganizationWebSupport() {
	}

	static AccountId accountId(Authentication authentication) {
		AccountPrincipal principal = requirePrincipal(authentication);
		return AccountId.of(principal.accountUuid());
	}

	static AccountPrincipal requirePrincipal(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal;
	}

	static UUID requireUuid(String value) {
		return UUID.fromString(value);
	}

}
