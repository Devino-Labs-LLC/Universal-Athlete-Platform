package com.devinolabs.uap.identity.domain;

public interface TokenDigester {

	String digest(String rawToken);

}
