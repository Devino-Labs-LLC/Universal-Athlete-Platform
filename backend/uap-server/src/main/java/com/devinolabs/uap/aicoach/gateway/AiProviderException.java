package com.devinolabs.uap.aicoach.gateway;

/**
 * Provider failure that carries no response body and no credential material.
 */
public class AiProviderException extends RuntimeException {

	public enum Kind {
		TIMEOUT,
		UNAVAILABLE,
		MALFORMED
	}

	private final Kind kind;

	public AiProviderException(Kind kind) {
		super(kind.name());
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

}
