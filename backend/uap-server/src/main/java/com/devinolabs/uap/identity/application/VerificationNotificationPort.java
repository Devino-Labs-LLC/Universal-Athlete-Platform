package com.devinolabs.uap.identity.application;

import com.devinolabs.uap.identity.domain.EmailAddress;

public interface VerificationNotificationPort {

	void sendVerificationMessage(EmailAddress email, String rawVerificationToken);

}
