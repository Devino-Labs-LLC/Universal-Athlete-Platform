package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinitionCategory;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;
import com.devinolabs.uap.training.domain.ExerciseDifficulty;
import com.devinolabs.uap.training.domain.ExerciseLaterality;
import com.devinolabs.uap.training.domain.ExerciseMetricMode;
import com.devinolabs.uap.training.domain.ImpactLevel;
import com.devinolabs.uap.training.domain.MovementPattern;
import com.devinolabs.uap.training.domain.MuscleGroup;

interface ExerciseDefinitionJpaRepository extends JpaRepository<ExerciseDefinitionJpaEntity, UUID> {

	@Query("""
			select d from ExerciseDefinitionJpaEntity d
			where d.id = :id
			and (d.scope = com.devinolabs.uap.training.domain.ExerciseDefinitionScope.SYSTEM
				or d.athleteId = :athleteId)
			""")
	Optional<ExerciseDefinitionJpaEntity> findAccessible(
			@Param("id") UUID id,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select count(d) from ExerciseDefinitionJpaEntity d
			where d.scope = com.devinolabs.uap.training.domain.ExerciseDefinitionScope.SYSTEM
			and d.active = true
			and d.normalizedName = :normalizedName
			""")
	long countActiveSystemByNormalizedName(@Param("normalizedName") String normalizedName);

	@Query("""
			select count(d) from ExerciseDefinitionJpaEntity d
			where d.scope = com.devinolabs.uap.training.domain.ExerciseDefinitionScope.ATHLETE_CUSTOM
			and d.active = true
			and d.athleteId = :athleteId
			and d.normalizedName = :normalizedName
			and (:excludingId is null or d.id <> :excludingId)
			""")
	long countActiveCustomByAthleteIdAndNormalizedName(
			@Param("athleteId") UUID athleteId,
			@Param("normalizedName") String normalizedName,
			@Param("excludingId") UUID excludingId);

	@Query("""
			select d from ExerciseDefinitionJpaEntity d
			where d.active = true
			and (d.scope = com.devinolabs.uap.training.domain.ExerciseDefinitionScope.SYSTEM
				or d.athleteId = :athleteId)
			and (:scope is null or d.scope = :scope)
			and (:nameContains is null or d.normalizedName like concat('%', :nameContains, '%'))
			and (:category is null or d.category = :category)
			and (:metricMode is null or d.metricMode = :metricMode)
			and (:movementPattern is null or d.primaryMovementPattern = :movementPattern
				or :movementPattern member of d.secondaryMovementPatterns)
			and (:muscleGroup is null or :muscleGroup member of d.primaryMuscleGroups
				or :muscleGroup member of d.secondaryMuscleGroups)
			and (:equipment is null or :equipment member of d.requiredEquipment
				or :equipment member of d.optionalEquipment)
			and (:laterality is null or d.laterality = :laterality)
			and (:impactLevel is null or d.impactLevel = :impactLevel)
			and (:difficulty is null or d.difficulty = :difficulty)
			order by d.canonicalName asc, d.id asc
			""")
	Page<ExerciseDefinitionJpaEntity> findAccessibleActive(
			@Param("athleteId") UUID athleteId,
			@Param("nameContains") String nameContains,
			@Param("scope") ExerciseDefinitionScope scope,
			@Param("category") ExerciseDefinitionCategory category,
			@Param("metricMode") ExerciseMetricMode metricMode,
			@Param("movementPattern") MovementPattern movementPattern,
			@Param("muscleGroup") MuscleGroup muscleGroup,
			@Param("equipment") EquipmentType equipment,
			@Param("laterality") ExerciseLaterality laterality,
			@Param("impactLevel") ImpactLevel impactLevel,
			@Param("difficulty") ExerciseDifficulty difficulty,
			Pageable pageable);

	List<ExerciseDefinitionJpaEntity> findAllByIdIn(List<UUID> ids);

	@Query("""
			select d.id, equipment
			from ExerciseDefinitionJpaEntity d
			left join d.requiredEquipment equipment
			where d.id in :ids
			and d.active = true
			and (d.scope = com.devinolabs.uap.training.domain.ExerciseDefinitionScope.SYSTEM
				or d.athleteId = :athleteId)
			""")
	List<Object[]> findAccessibleActiveRequiredEquipmentRows(
			@Param("ids") List<UUID> ids,
			@Param("athleteId") UUID athleteId);

}
