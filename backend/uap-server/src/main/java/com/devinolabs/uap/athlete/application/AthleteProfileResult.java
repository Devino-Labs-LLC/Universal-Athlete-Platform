package com.devinolabs.uap.athlete.application;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteStatus;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;

public final class AthleteProfileResult {

	private final AthleteId id;
	private final String firstName;
	private final String lastName;
	private final LocalDate dateOfBirth;
	private final Sex sex;
	private final Height height;
	private final Weight weight;
	private final DominantHand dominantHand;
	private final DominantFoot dominantFoot;
	private final AthleteStatus status;
	private final Instant createdAt;
	private final Instant updatedAt;

	public AthleteProfileResult(
			AthleteId id,
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
			Instant updatedAt) {
		this.id = Objects.requireNonNull(id);
		this.firstName = Objects.requireNonNull(firstName);
		this.lastName = Objects.requireNonNull(lastName);
		this.dateOfBirth = Objects.requireNonNull(dateOfBirth);
		this.sex = Objects.requireNonNull(sex);
		this.height = Objects.requireNonNull(height);
		this.weight = Objects.requireNonNull(weight);
		this.dominantHand = Objects.requireNonNull(dominantHand);
		this.dominantFoot = Objects.requireNonNull(dominantFoot);
		this.status = Objects.requireNonNull(status);
		this.createdAt = Objects.requireNonNull(createdAt);
		this.updatedAt = Objects.requireNonNull(updatedAt);
	}

	public AthleteId id() {
		return id;
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

}
