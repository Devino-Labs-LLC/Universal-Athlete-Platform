package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ExerciseSubstitutionRelationshipJpaRepository
		extends JpaRepository<ExerciseSubstitutionRelationshipJpaEntity, UUID> {

	Optional<ExerciseSubstitutionRelationshipJpaEntity> findByIdAndActiveTrue(UUID id);

	@Query("""
			select r from ExerciseSubstitutionRelationshipJpaEntity r
			where r.id = :id
			and (r.ownerAthleteId is null or r.ownerAthleteId = :athleteId)
			""")
	Optional<ExerciseSubstitutionRelationshipJpaEntity> findAccessible(
			@Param("id") UUID id,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select r from ExerciseSubstitutionRelationshipJpaEntity r
			where r.sourceExerciseDefinitionId = :sourceDefinitionId
			and r.active = true
			and (r.ownerAthleteId is null or r.ownerAthleteId = :athleteId)
			order by
				case r.compatibilityLevel
					when com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility.HIGH then 0
					when com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility.MODERATE then 1
					when com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility.CONDITIONAL then 2
					else 3
				end,
				r.relationshipType asc,
				r.id asc
			""")
	List<ExerciseSubstitutionRelationshipJpaEntity> findActiveBySourceDefinitionId(
			@Param("sourceDefinitionId") UUID sourceDefinitionId,
			@Param("athleteId") UUID athleteId);

	@Query("""
			select r from ExerciseSubstitutionRelationshipJpaEntity r
			where r.sourceExerciseDefinitionId in :sourceDefinitionIds
			and r.active = true
			and (r.ownerAthleteId is null or r.ownerAthleteId = :athleteId)
			order by
				r.sourceExerciseDefinitionId asc,
				case r.compatibilityLevel
					when com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility.HIGH then 0
					when com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility.MODERATE then 1
					when com.devinolabs.uap.training.domain.ExerciseSubstitutionCompatibility.CONDITIONAL then 2
					else 3
				end,
				r.relationshipType asc,
				r.id asc
			""")
	List<ExerciseSubstitutionRelationshipJpaEntity> findActiveBySourceDefinitionIds(
			@Param("sourceDefinitionIds") Collection<UUID> sourceDefinitionIds,
			@Param("athleteId") UUID athleteId);

}
