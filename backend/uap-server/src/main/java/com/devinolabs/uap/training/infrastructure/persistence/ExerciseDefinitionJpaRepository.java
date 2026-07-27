package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;

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
			order by d.canonicalName asc, d.id asc
			""")
	Page<ExerciseDefinitionJpaEntity> findAccessibleActive(
			@Param("athleteId") UUID athleteId,
			@Param("nameContains") String nameContains,
			@Param("scope") ExerciseDefinitionScope scope,
			Pageable pageable);

}
