package com.devinolabs.uap.identity.domain;

public interface PasswordHasher {

	String hash(CharSequence rawPassword);

	boolean matches(CharSequence rawPassword, PasswordCredential credential);

}
