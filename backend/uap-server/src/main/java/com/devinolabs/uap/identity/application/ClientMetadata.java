package com.devinolabs.uap.identity.application;

public record ClientMetadata(String ipAddress, String userAgent) {

	public static ClientMetadata empty() {
		return new ClientMetadata(null, null);
	}

}
