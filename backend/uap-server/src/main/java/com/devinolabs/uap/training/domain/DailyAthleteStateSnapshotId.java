package com.devinolabs.uap.training.domain;

import java.util.Objects;
import java.util.UUID;

public final class DailyAthleteStateSnapshotId {

	private final UUID value;

	private DailyAthleteStateSnapshotId(UUID value) {
		this.value = Objects.requireNonNull(value, "value must not be null");
	}

	public static DailyAthleteStateSnapshotId of(UUID value) {
		return new DailyAthleteStateSnapshotId(value);
	}

	public static DailyAthleteStateSnapshotId of(String value) {
		return of(UUID.fromString(value));
	}

	public static DailyAthleteStateSnapshotId generate() {
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
		if (!(other instanceof DailyAthleteStateSnapshotId that)) {
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
