package com.devinolabs.uap.identity.api;

/**
 * Published token digest for modules that must store hashed secrets (invitations, etc.).
 */
public interface SecureTokenDigestPort {

	String digest(String rawToken);

}
