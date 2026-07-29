package com.devinolabs.uap.training.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable catalogue metadata attached to an {@link ExerciseDefinition}.
 */
public final class ExerciseDefinitionMetadata {

	private final ExerciseDefinitionCategory category;
	private final ExerciseMetricMode metricMode;
	private final MovementPattern primaryMovementPattern;
	private final List<MovementPattern> secondaryMovementPatterns;
	private final List<MuscleGroup> primaryMuscleGroups;
	private final List<MuscleGroup> secondaryMuscleGroups;
	private final List<EquipmentType> requiredEquipment;
	private final List<EquipmentType> optionalEquipment;
	private final ExerciseLaterality laterality;
	private final KineticChainType kineticChainType;
	private final ImpactLevel impactLevel;
	private final ExerciseDifficulty difficulty;

	private ExerciseDefinitionMetadata(
			ExerciseDefinitionCategory category,
			ExerciseMetricMode metricMode,
			MovementPattern primaryMovementPattern,
			List<MovementPattern> secondaryMovementPatterns,
			List<MuscleGroup> primaryMuscleGroups,
			List<MuscleGroup> secondaryMuscleGroups,
			List<EquipmentType> requiredEquipment,
			List<EquipmentType> optionalEquipment,
			ExerciseLaterality laterality,
			KineticChainType kineticChainType,
			ImpactLevel impactLevel,
			ExerciseDifficulty difficulty) {
		this.category = Objects.requireNonNull(category, "category must not be null");
		this.metricMode = Objects.requireNonNull(metricMode, "metricMode must not be null");
		this.primaryMovementPattern = Objects.requireNonNull(
				primaryMovementPattern, "primaryMovementPattern must not be null");
		this.secondaryMovementPatterns = List.copyOf(secondaryMovementPatterns);
		this.primaryMuscleGroups = List.copyOf(primaryMuscleGroups);
		this.secondaryMuscleGroups = List.copyOf(secondaryMuscleGroups);
		this.requiredEquipment = List.copyOf(requiredEquipment);
		this.optionalEquipment = List.copyOf(optionalEquipment);
		this.laterality = Objects.requireNonNull(laterality, "laterality must not be null");
		this.kineticChainType = Objects.requireNonNull(kineticChainType, "kineticChainType must not be null");
		this.impactLevel = Objects.requireNonNull(impactLevel, "impactLevel must not be null");
		this.difficulty = Objects.requireNonNull(difficulty, "difficulty must not be null");
		ExerciseDefinitionMetadataValidator.validate(this);
	}

	public static ExerciseDefinitionMetadata of(
			ExerciseDefinitionCategory category,
			ExerciseMetricMode metricMode,
			MovementPattern primaryMovementPattern,
			Collection<MovementPattern> secondaryMovementPatterns,
			Collection<MuscleGroup> primaryMuscleGroups,
			Collection<MuscleGroup> secondaryMuscleGroups,
			Collection<EquipmentType> requiredEquipment,
			Collection<EquipmentType> optionalEquipment,
			ExerciseLaterality laterality,
			KineticChainType kineticChainType,
			ImpactLevel impactLevel,
			ExerciseDifficulty difficulty) {
		return new ExerciseDefinitionMetadata(
				category,
				metricMode,
				primaryMovementPattern,
				orderedUnique(secondaryMovementPatterns, MovementPattern.class),
				orderedUnique(primaryMuscleGroups, MuscleGroup.class),
				orderedUnique(secondaryMuscleGroups, MuscleGroup.class),
				orderedUnique(requiredEquipment, EquipmentType.class),
				orderedUnique(optionalEquipment, EquipmentType.class),
				laterality,
				kineticChainType,
				impactLevel,
				difficulty);
	}

	/**
	 * Documented migration placeholder for custom definitions that pre-date catalogue metadata.
	 */
	public static ExerciseDefinitionMetadata legacyPlaceholder() {
		return of(
				ExerciseDefinitionCategory.OTHER,
				ExerciseMetricMode.MIXED,
				MovementPattern.OTHER,
				List.of(),
				List.of(MuscleGroup.OTHER),
				List.of(),
				List.of(),
				List.of(),
				ExerciseLaterality.NOT_APPLICABLE,
				KineticChainType.NOT_APPLICABLE,
				ImpactLevel.LOW_IMPACT,
				ExerciseDifficulty.BEGINNER);
	}

	private static <E extends Enum<E>> List<E> orderedUnique(Collection<E> values, Class<E> type) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		EnumSet<E> seen = EnumSet.noneOf(type);
		List<E> ordered = new ArrayList<>();
		for (E value : values) {
			if (value == null) {
				throw new InvalidExerciseDefinitionMetadataException("Metadata collections must not contain null");
			}
			if (!seen.add(value)) {
				throw new InvalidExerciseDefinitionMetadataException(
						"Duplicate " + type.getSimpleName() + " value: " + value);
			}
			ordered.add(value);
		}
		ordered.sort(Comparator.comparingInt(Enum::ordinal));
		return ordered;
	}

	public ExerciseDefinitionCategory category() {
		return category;
	}

	public ExerciseMetricMode metricMode() {
		return metricMode;
	}

	public MovementPattern primaryMovementPattern() {
		return primaryMovementPattern;
	}

	public List<MovementPattern> secondaryMovementPatterns() {
		return secondaryMovementPatterns;
	}

	public List<MuscleGroup> primaryMuscleGroups() {
		return primaryMuscleGroups;
	}

	public List<MuscleGroup> secondaryMuscleGroups() {
		return secondaryMuscleGroups;
	}

	public List<EquipmentType> requiredEquipment() {
		return requiredEquipment;
	}

	public List<EquipmentType> optionalEquipment() {
		return optionalEquipment;
	}

	public ExerciseLaterality laterality() {
		return laterality;
	}

	public KineticChainType kineticChainType() {
		return kineticChainType;
	}

	public ImpactLevel impactLevel() {
		return impactLevel;
	}

	public ExerciseDifficulty difficulty() {
		return difficulty;
	}

	public Set<MovementPattern> allMovementPatterns() {
		LinkedHashSet<MovementPattern> all = new LinkedHashSet<>();
		all.add(primaryMovementPattern);
		all.addAll(secondaryMovementPatterns);
		return Set.copyOf(all);
	}

	public Set<MuscleGroup> allMuscleGroups() {
		LinkedHashSet<MuscleGroup> all = new LinkedHashSet<>();
		all.addAll(primaryMuscleGroups);
		all.addAll(secondaryMuscleGroups);
		return Set.copyOf(all);
	}

	public Set<EquipmentType> allEquipment() {
		LinkedHashSet<EquipmentType> all = new LinkedHashSet<>();
		all.addAll(requiredEquipment);
		all.addAll(optionalEquipment);
		return Set.copyOf(all);
	}

}
