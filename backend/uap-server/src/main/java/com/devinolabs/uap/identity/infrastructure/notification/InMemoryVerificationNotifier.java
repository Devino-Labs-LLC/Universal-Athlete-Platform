package com.devinolabs.uap.identity.infrastructure.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.identity.application.VerificationNotificationPort;
import com.devinolabs.uap.identity.domain.EmailAddress;

@Component
public class InMemoryVerificationNotifier implements VerificationNotificationPort {

	private final List<VerificationMessage> messages = new ArrayList<>();

	@Override
	public synchronized void sendVerificationMessage(EmailAddress email, String rawVerificationToken) {
		if (email == null) {
			throw new IllegalArgumentException("email must not be null");
		}
		if (rawVerificationToken == null || rawVerificationToken.isBlank()) {
			throw new IllegalArgumentException("rawVerificationToken must not be blank");
		}
		messages.add(new VerificationMessage(email, rawVerificationToken));
	}

	public synchronized Optional<VerificationMessage> lastMessage() {
		if (messages.isEmpty()) {
			return Optional.empty();
		}
		return Optional.of(messages.get(messages.size() - 1));
	}

	public synchronized List<VerificationMessage> messages() {
		return List.copyOf(messages);
	}

	public synchronized void clear() {
		messages.clear();
	}

	public record VerificationMessage(EmailAddress email, String rawToken) {
	}

}
