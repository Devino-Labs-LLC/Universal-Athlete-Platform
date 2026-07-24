package com.devinolabs.uap.identity.application;

import com.devinolabs.uap.identity.domain.EmailAddress;

public class DuplicateAccountEmailException extends RuntimeException {

	private final EmailAddress email;

	public DuplicateAccountEmailException(EmailAddress email) {
		super("An account already exists for email: " + email.value());
		this.email = email;
	}

	public DuplicateAccountEmailException(EmailAddress email, Throwable cause) {
		super("An account already exists for email: " + email.value(), cause);
		this.email = email;
	}

	public EmailAddress email() {
		return email;
	}

}
