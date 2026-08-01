package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class DailyRecoveryCheckInId {

	private final UUID value;

	private DailyRecoveryCheckInId(UUID value) {
		this.value = Objects.requireNonNull(value, "value must not be null");
	}

	public static DailyRecoveryCheckInId of(UUID value) {
		return new DailyRecoveryCheckInId(value);
	}

	public static DailyRecoveryCheckInId of(String value) {
		return of(UUID.fromString(value));
	}

	public static DailyRecoveryCheckInId generate() {
		return of(UUID.randomUUID());
	}

	public UUID value() {
		return value;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof DailyRecoveryCheckInId that)) {
			return false;
		}
		return value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return value.hashCode();
	}

	@Override
	public String toString() {
		return value.toString();
	}

}
