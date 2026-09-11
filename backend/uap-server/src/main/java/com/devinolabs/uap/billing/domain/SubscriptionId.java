package com.devinolabs.uap.billing.domain;

import java.util.Objects;
import java.util.UUID;

public record SubscriptionId(UUID value) {

	public SubscriptionId {
		Objects.requireNonNull(value, "SubscriptionId value must not be null");
	}

	public static SubscriptionId generate() {
		return new SubscriptionId(UUID.randomUUID());
	}

	public static SubscriptionId of(UUID value) {
		return new SubscriptionId(value);
	}

}
