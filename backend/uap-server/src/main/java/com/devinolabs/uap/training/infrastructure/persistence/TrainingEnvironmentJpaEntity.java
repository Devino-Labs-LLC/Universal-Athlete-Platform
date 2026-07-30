package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.persistence.Version;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

@Entity
@Table(name = "training_environments")
class TrainingEnvironmentJpaEntity implements Persistable<UUID> {
	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;
	@Column(name = "name", nullable = false, length = 100)
	private String name;
	@Column(name = "normalized_name", nullable = false, length = 100)
	private String normalizedName;
	@Enumerated(EnumType.STRING)
	@Column(name = "environment_type", nullable = false, length = 32)
	private TrainingEnvironmentType environmentType;
	@Column(name = "description", length = 2000)
	private String description;
	@Column(name = "facility_notes", length = 2000)
	private String facilityNotes;
	@Column(name = "default_environment", nullable = false)
	private boolean defaultEnvironment;
	@Column(name = "active", nullable = false)
	private boolean active;
	@Column(name = "archived_at")
	private Instant archivedAt;
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;
	@Version
	@Column(name = "version", nullable = false)
	private long version;
	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(name = "training_environment_equipment",
			joinColumns = @JoinColumn(name = "training_environment_id", columnDefinition = "BINARY(16)"))
	@Column(name = "equipment_type", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<EquipmentType> availableEquipment = new LinkedHashSet<>();
	@Transient private boolean isNew = true;
	protected TrainingEnvironmentJpaEntity() {}
	TrainingEnvironmentJpaEntity(UUID id, UUID athleteId, String name, String normalizedName,
			TrainingEnvironmentType environmentType, String description, String facilityNotes,
			boolean defaultEnvironment, boolean active, Instant archivedAt, Instant createdAt, Instant updatedAt,
			long version, Set<EquipmentType> availableEquipment, boolean isNew) {
		this.id = id; this.athleteId = athleteId; this.name = name; this.normalizedName = normalizedName;
		this.environmentType = environmentType; this.description = description; this.facilityNotes = facilityNotes;
		this.defaultEnvironment = defaultEnvironment; this.active = active; this.archivedAt = archivedAt;
		this.createdAt = createdAt; this.updatedAt = updatedAt; this.version = version;
		this.availableEquipment = availableEquipment == null ? new LinkedHashSet<>() : new LinkedHashSet<>(availableEquipment);
		this.isNew = isNew;
	}
	@Override public UUID getId() { return id; }
	@Override public boolean isNew() { return isNew; }
	@PostLoad @PostPersist void markNotNew() { this.isNew = false; }
	UUID getAthleteId() { return athleteId; }
	String getName() { return name; }
	String getNormalizedName() { return normalizedName; }
	TrainingEnvironmentType getEnvironmentType() { return environmentType; }
	String getDescription() { return description; }
	String getFacilityNotes() { return facilityNotes; }
	boolean isDefaultEnvironment() { return defaultEnvironment; }
	boolean isActive() { return active; }
	Instant getArchivedAt() { return archivedAt; }
	Instant getCreatedAt() { return createdAt; }
	Instant getUpdatedAt() { return updatedAt; }
	long getVersion() { return version; }
	Set<EquipmentType> getAvailableEquipment() { return availableEquipment; }
	void setName(String name) { this.name = name; }
	void setNormalizedName(String normalizedName) { this.normalizedName = normalizedName; }
	void setEnvironmentType(TrainingEnvironmentType environmentType) { this.environmentType = environmentType; }
	void setDescription(String description) { this.description = description; }
	void setFacilityNotes(String facilityNotes) { this.facilityNotes = facilityNotes; }
	void setDefaultEnvironment(boolean defaultEnvironment) { this.defaultEnvironment = defaultEnvironment; }
	void setActive(boolean active) { this.active = active; }
	void setArchivedAt(Instant archivedAt) { this.archivedAt = archivedAt; }
	void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
	void setAvailableEquipment(Set<EquipmentType> availableEquipment) {
		this.availableEquipment.clear();
		if (availableEquipment != null) this.availableEquipment.addAll(availableEquipment);
	}
}
