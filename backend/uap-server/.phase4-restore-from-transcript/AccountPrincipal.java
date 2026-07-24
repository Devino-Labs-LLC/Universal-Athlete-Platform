package com.devinolabs.uap.identity.infrastructure.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.AuthenticatedPrincipal;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.devinolabs.uap.identity.domain.AccountId;

public final class AccountPrincipal implements AuthenticatedPrincipal {

	private final AccountId accountId;

	public AccountPrincipal(AccountId accountId) {
		this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
	}

	public AccountId accountId() {
		return accountId;
	}

	@Override
	public String getName() {
		return accountId.value().toString();
	}

	public Collection<? extends GrantedAuthority> authorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_USER"));
	}

}
