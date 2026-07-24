package com.devinolabs.uap.athlete.domain;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

public class Athlete {

	private final AthleteId id;
	private final AccountId accountId;
	private String firstName;
	private String lastName;
	private LocalDate dateOfBirth;
	private Sex sex;
	private Height height;
	private Weight weight;
	private DominantHand dominantHand;
	private DominantFoot dominantFoot;
	private AthleteStatus status;
	private final Instant createdAt;
	private Instant updatedAt;
	private long version;

	private Athlete(
			AthleteId id,
			AccountId accountId,
			String firstName,
			String lastName,
			LocalDate dateOfBirth,
			Sex sex,
			Height height,
			Weight weight,
			DominantHand dominantHand,
			DominantFoot dominantFoot,
			AthleteStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "Athlete id must not be null");
		this.accountId = Objects.requireNonNull(accountId, "Athlete accountId must not be null");
		this.firstName = requireName(firstName, "firstName");
		this.lastName = requireName(lastName, "lastName");
		this.dateOfBirth = Objects.requireNonNull(dateOfBirth, "Athlete dateOfBirth must not be null");
		this.sex = Objects.requireNonNull(sex, "Athlete sex must not be null");
		this.height = Objects.requireNonNull(height, "Athlete height must not be null");
		this.weight = Objects.requireNonNull(weight, "Athlete weight must not be null");
		this.dominantHand = Objects.requireNonNull(dominantHand, "Athlete dominantHand must not be null");
		this.dominantFoot = Objects.requireNonNull(dominantFoot, "Athlete dominantFoot must not be null");
		this.status = Objects.requireNonNull(status, "Athlete status must not be null");
		this.createdAt = Objects.requireNonNull(createdAt, "Athlete createdAt must not be null");
		this.updatedAt = Objects.requireNonNull(updatedAt, "Athlete updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static Athlete register(
			AthleteId id,
			AccountId accountId,
			String firstName,
			String lastName,
			LocalDate dateOfBirth,
			Sex sex,
			Height height,
			Weight weight,
			DominantHand dominantHand,
			DominantFoot dominantFoot) {
		return register(
				id,
				accountId,
				firstName,
				lastName,
				dateOfBirth,
				sex,
				height,
				weight,
				dominantHand,
				dominantFoot,
				Clock.systemUTC());
	}

	public static Athlete register(
			AthleteId id,
			AccountId accountId,
			String firstName,
			String lastName,
			LocalDate dateOfBirth,
			Sex sex,
			Height height,
			Weight weight,
			DominantHand dominantHand,
			DominantFoot dominantFoot,
			Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		Instant now = Instant.now(clock);
		return new Athlete(
				id,
				accountId,
				firstName,
				lastName,
				dateOfBirth,
				sex,
				height,
				weight,
				dominantHand,
				dominantFoot,
				AthleteStatus.ACTIVE,
				now,
				now,
				0L);
	}

	public static Athlete rehydrate(
			AthleteId id,
			AccountId accountId,
			String firstName,
			String lastName,
			LocalDate dateOfBirth,
			Sex sex,
			Height height,
			Weight weight,
			DominantHand dominantHand,
			DominantFoot dominantFoot,
			AthleteStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version) {
		return new Athlete(
				id,
				accountId,
				firstName,
				lastName,
				dateOfBirth,
				sex,
				height,
				weight,
				dominantHand,
				dominantFoot,
				status,
				createdAt,
				updatedAt,
				version);
	}

	public void rename(String firstName, String lastName, Clock clock) {
		requireMutable(clock);
		this.firstName = requireName(firstName, "firstName");
		this.lastName = requireName(lastName, "lastName");
		touch(clock);
	}

	public void updateHeight(Height height, Clock clock) {
		requireMutable(clock);
		this.height = Objects.requireNonNull(height, "Athlete height must not be null");
		touch(clock);
	}

	public void updateWeight(Weight weight, Clock clock) {
		requireMutable(clock);
		this.weight = Objects.requireNonNull(weight, "Athlete weight must not be null");
		touch(clock);
	}

	public void updateDominantHand(DominantHand dominantHand, Clock clock) {
		requireMutable(clock);
		this.dominantHand = Objects.requireNonNull(dominantHand, "Athlete dominantHand must not be null");
		touch(clock);
	}

	public void updateDominantFoot(DominantFoot dominantFoot, Clock clock) {
		requireMutable(clock);
		this.dominantFoot = Objects.requireNonNull(dominantFoot, "Athlete dominantFoot must not be null");
		touch(clock);
	}

	public void archive(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == AthleteStatus.ARCHIVED) {
			throw new IllegalStateException("Athlete is already archived");
		}
		this.status = AthleteStatus.ARCHIVED;
		touch(clock);
	}

	public void reactivate(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == AthleteStatus.ACTIVE) {
			throw new IllegalStateException("Athlete is already active");
		}
		this.status = AthleteStatus.ACTIVE;
		touch(clock);
	}

	private void requireMutable(Clock clock) {
		Objects.requireNonNull(clock, "Clock must not be null");
		if (status == AthleteStatus.ARCHIVED) {
			throw new IllegalStateException("Archived athlete cannot be modified");
		}
	}

	private void touch(Clock clock) {
		this.updatedAt = Instant.now(clock);
	}

	private static String requireName(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		String normalized = value.trim();
		if (normalized.length() > 100) {
			throw new IllegalArgumentException(fieldName + " must not exceed 100 characters");
		}
		return normalized;
	}

	public AthleteId id() {
		return id;
	}

	public AccountId accountId() {
		return accountId;
	}

	public String firstName() {
		return firstName;
	}

	public String lastName() {
		return lastName;
	}

	public LocalDate dateOfBirth() {
		return dateOfBirth;
	}

	public Sex sex() {
		return sex;
	}

	public Height height() {
		return height;
	}

	public Weight weight() {
		return weight;
	}

	public DominantHand dominantHand() {
		return dominantHand;
	}

	public DominantFoot dominantFoot() {
		return dominantFoot;
	}

	public AthleteStatus status() {
		return status;
	}

	public Instant createdAt() {
		return createdAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public long version() {
		return version;
	}

}
