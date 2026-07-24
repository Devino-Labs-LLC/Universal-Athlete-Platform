package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.athlete.domain.AthleteStatus;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Sex;

@Entity
@Table(name = "athletes")
class AthleteJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "account_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID accountId;

	@Column(name = "first_name", nullable = false, length = 100)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 100)
	private String lastName;

	@Column(name = "date_of_birth", nullable = false)
	private LocalDate dateOfBirth;

	@Enumerated(EnumType.STRING)
	@Column(name = "sex", nullable = false, length = 20)
	private Sex sex;

	@Column(name = "height_cm", nullable = false, precision = 5, scale = 2)
	private BigDecimal heightCm;

	@Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
	private BigDecimal weightKg;

	@Enumerated(EnumType.STRING)
	@Column(name = "dominant_hand", nullable = false, length = 20)
	private DominantHand dominantHand;

	@Enumerated(EnumType.STRING)
	@Column(name = "dominant_foot", nullable = false, length = 20)
	private DominantFoot dominantFoot;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 20)
	private AthleteStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	@Column(name = "version", nullable = false)
	private long version;

	@Transient
	private boolean isNew = true;

	protected AthleteJpaEntity() {
	}

	AthleteJpaEntity(
			UUID id,
			UUID accountId,
			String firstName,
			String lastName,
			LocalDate dateOfBirth,
			Sex sex,
			BigDecimal heightCm,
			BigDecimal weightKg,
			DominantHand dominantHand,
			DominantFoot dominantFoot,
			AthleteStatus status,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.accountId = accountId;
		this.firstName = firstName;
		this.lastName = lastName;
		this.dateOfBirth = dateOfBirth;
		this.sex = sex;
		this.heightCm = heightCm;
		this.weightKg = weightKg;
		this.dominantHand = dominantHand;
		this.dominantFoot = dominantFoot;
		this.status = status;
		this.createdAt = createdAt;
		this.updatedAt = updatedAt;
		this.version = version;
		this.isNew = isNew;
	}

	@Override
	public UUID getId() {
		return id;
	}

	@Override
	public boolean isNew() {
		return isNew;
	}

	@PostLoad
	@PostPersist
	void markNotNew() {
		this.isNew = false;
	}

	UUID getAccountId() {
		return accountId;
	}

	String getFirstName() {
		return firstName;
	}

	String getLastName() {
		return lastName;
	}

	LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	Sex getSex() {
		return sex;
	}

	BigDecimal getHeightCm() {
		return heightCm;
	}

	BigDecimal getWeightKg() {
		return weightKg;
	}

	DominantHand getDominantHand() {
		return dominantHand;
	}

	DominantFoot getDominantFoot() {
		return dominantFoot;
	}

	AthleteStatus getStatus() {
		return status;
	}

	Instant getCreatedAt() {
		return createdAt;
	}

	Instant getUpdatedAt() {
		return updatedAt;
	}

	long getVersion() {
		return version;
	}

}
