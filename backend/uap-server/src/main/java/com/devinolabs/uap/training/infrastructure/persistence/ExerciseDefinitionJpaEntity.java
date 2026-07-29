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
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.KineticChainType;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;

@Entity
@Table(name = "exercise_definitions")
class ExerciseDefinitionJpaEntity implements Persistable<UUID> {

	@Id
	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "id", nullable = false, updatable = false, columnDefinition = "BINARY(16)")
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(name = "scope", nullable = false, updatable = false, length = 20)
	private ExerciseDefinitionScope scope;

	@JdbcTypeCode(SqlTypes.UUID)
	@Column(name = "athlete_id", updatable = false, columnDefinition = "BINARY(16)")
	private UUID athleteId;

	@Column(name = "canonical_name", nullable = false, length = 150)
	private String canonicalName;

	@Column(name = "normalized_name", nullable = false, length = 150)
	private String normalizedName;

	@Enumerated(EnumType.STRING)
	@Column(name = "category", nullable = false, length = 32)
	private ExerciseDefinitionCategory category;

	@Enumerated(EnumType.STRING)
	@Column(name = "metric_mode", nullable = false, length = 32)
	private ExerciseMetricMode metricMode;

	@Enumerated(EnumType.STRING)
	@Column(name = "primary_movement_pattern", nullable = false, length = 40)
	private MovementPattern primaryMovementPattern;

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "exercise_definition_secondary_movement_patterns",
			joinColumns = @JoinColumn(name = "exercise_definition_id", columnDefinition = "BINARY(16)"))
	@Column(name = "movement_pattern", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<MovementPattern> secondaryMovementPatterns = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "exercise_definition_primary_muscle_groups",
			joinColumns = @JoinColumn(name = "exercise_definition_id", columnDefinition = "BINARY(16)"))
	@Column(name = "muscle_group", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<MuscleGroup> primaryMuscleGroups = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "exercise_definition_secondary_muscle_groups",
			joinColumns = @JoinColumn(name = "exercise_definition_id", columnDefinition = "BINARY(16)"))
	@Column(name = "muscle_group", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<MuscleGroup> secondaryMuscleGroups = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "exercise_definition_required_equipment",
			joinColumns = @JoinColumn(name = "exercise_definition_id", columnDefinition = "BINARY(16)"))
	@Column(name = "equipment_type", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<EquipmentType> requiredEquipment = new LinkedHashSet<>();

	@ElementCollection(fetch = FetchType.EAGER)
	@CollectionTable(
			name = "exercise_definition_optional_equipment",
			joinColumns = @JoinColumn(name = "exercise_definition_id", columnDefinition = "BINARY(16)"))
	@Column(name = "equipment_type", nullable = false, length = 40)
	@Enumerated(EnumType.STRING)
	private Set<EquipmentType> optionalEquipment = new LinkedHashSet<>();

	@Enumerated(EnumType.STRING)
	@Column(name = "laterality", nullable = false, length = 32)
	private ExerciseLaterality laterality;

	@Enumerated(EnumType.STRING)
	@Column(name = "kinetic_chain_type", nullable = false, length = 32)
	private KineticChainType kineticChainType;

	@Enumerated(EnumType.STRING)
	@Column(name = "impact_level", nullable = false, length = 32)
	private ImpactLevel impactLevel;

	@Enumerated(EnumType.STRING)
	@Column(name = "difficulty", nullable = false, length = 32)
	private ExerciseDifficulty difficulty;

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

	@Transient
	private boolean isNew = true;

	protected ExerciseDefinitionJpaEntity() {
	}

	ExerciseDefinitionJpaEntity(
			UUID id,
			ExerciseDefinitionScope scope,
			UUID athleteId,
			String canonicalName,
			String normalizedName,
			ExerciseDefinitionCategory category,
			ExerciseMetricMode metricMode,
			MovementPattern primaryMovementPattern,
			Set<MovementPattern> secondaryMovementPatterns,
			Set<MuscleGroup> primaryMuscleGroups,
			Set<MuscleGroup> secondaryMuscleGroups,
			Set<EquipmentType> requiredEquipment,
			Set<EquipmentType> optionalEquipment,
			ExerciseLaterality laterality,
			KineticChainType kineticChainType,
			ImpactLevel impactLevel,
			ExerciseDifficulty difficulty,
			boolean active,
			Instant archivedAt,
			Instant createdAt,
			Instant updatedAt,
			long version,
			boolean isNew) {
		this.id = id;
		this.scope = scope;
		this.athleteId = athleteId;
		this.canonicalName = canonicalName;
		this.normalizedName = normalizedName;
		this.category = category;
		this.metricMode = metricMode;
		this.primaryMovementPattern = primaryMovementPattern;
		this.secondaryMovementPatterns = new LinkedHashSet<>(secondaryMovementPatterns);
		this.primaryMuscleGroups = new LinkedHashSet<>(primaryMuscleGroups);
		this.secondaryMuscleGroups = new LinkedHashSet<>(secondaryMuscleGroups);
		this.requiredEquipment = new LinkedHashSet<>(requiredEquipment);
		this.optionalEquipment = new LinkedHashSet<>(optionalEquipment);
		this.laterality = laterality;
		this.kineticChainType = kineticChainType;
		this.impactLevel = impactLevel;
		this.difficulty = difficulty;
		this.active = active;
		this.archivedAt = archivedAt;
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

	ExerciseDefinitionScope getScope() {
		return scope;
	}

	UUID getAthleteId() {
		return athleteId;
	}

	String getCanonicalName() {
		return canonicalName;
	}

	String getNormalizedName() {
		return normalizedName;
	}

	ExerciseDefinitionCategory getCategory() {
		return category;
	}

	ExerciseMetricMode getMetricMode() {
		return metricMode;
	}

	MovementPattern getPrimaryMovementPattern() {
		return primaryMovementPattern;
	}

	Set<MovementPattern> getSecondaryMovementPatterns() {
		return secondaryMovementPatterns;
	}

	Set<MuscleGroup> getPrimaryMuscleGroups() {
		return primaryMuscleGroups;
	}

	Set<MuscleGroup> getSecondaryMuscleGroups() {
		return secondaryMuscleGroups;
	}

	Set<EquipmentType> getRequiredEquipment() {
		return requiredEquipment;
	}

	Set<EquipmentType> getOptionalEquipment() {
		return optionalEquipment;
	}

	ExerciseLaterality getLaterality() {
		return laterality;
	}

	KineticChainType getKineticChainType() {
		return kineticChainType;
	}

	ImpactLevel getImpactLevel() {
		return impactLevel;
	}

	ExerciseDifficulty getDifficulty() {
		return difficulty;
	}

	boolean isActive() {
		return active;
	}

	Instant getArchivedAt() {
		return archivedAt;
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
