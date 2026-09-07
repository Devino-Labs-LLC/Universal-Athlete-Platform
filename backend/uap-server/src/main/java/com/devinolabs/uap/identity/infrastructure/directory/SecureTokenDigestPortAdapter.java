package com.devinolabs.uap.identity.infrastructure.directory;

import java.util.Objects;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.identity.api.SecureTokenDigestPort;
import com.devinolabs.uap.identity.domain.TokenDigester;

@Component
class SecureTokenDigestPortAdapter implements SecureTokenDigestPort {

	private final TokenDigester tokenDigester;

	SecureTokenDigestPortAdapter(TokenDigester tokenDigester) {
		this.tokenDigester = Objects.requireNonNull(tokenDigester);
	}

	@Override
	public String digest(String rawToken) {
		return tokenDigester.digest(rawToken);
	}

}
