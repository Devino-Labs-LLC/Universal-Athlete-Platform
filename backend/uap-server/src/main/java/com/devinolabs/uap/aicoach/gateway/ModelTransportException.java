package com.devinolabs.uap.aicoach.gateway;

public class ModelTransportException extends RuntimeException {

	public enum Kind {
		TIMEOUT,
		IO
	}

	private final Kind kind;

	public ModelTransportException(Kind kind) {
		super(kind.name());
		this.kind = kind;
	}

	public Kind kind() {
		return kind;
	}

}
