package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.ExerciseSubstitutionRelationshipRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationship;
import com.devinolabs.uap.training.domain.ExerciseSubstitutionRelationshipId;

@Repository
class JpaExerciseSubstitutionRelationshipRepository implements ExerciseSubstitutionRelationshipRepository {

	private final ExerciseSubstitutionRelationshipJpaRepository jpaRepository;

	JpaExerciseSubstitutionRelationshipRepository(ExerciseSubstitutionRelationshipJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public ExerciseSubstitutionRelationship save(ExerciseSubstitutionRelationship relationship) {
		boolean isNew = !jpaRepository.existsById(relationship.id().value());
		ExerciseSubstitutionRelationshipJpaEntity saved = jpaRepository.save(
				ExerciseSubstitutionRelationshipPersistenceMapper.toEntity(relationship, isNew));
		jpaRepository.flush();
		return ExerciseSubstitutionRelationshipPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<ExerciseSubstitutionRelationship> findById(ExerciseSubstitutionRelationshipId id) {
		return jpaRepository.findById(id.value()).map(ExerciseSubstitutionRelationshipPersistenceMapper::toDomain);
	}

	@Override
	public Optional<ExerciseSubstitutionRelationship> findAccessible(
			ExerciseSubstitutionRelationshipId id,
			AthleteId athleteId) {
		return jpaRepository
				.findAccessible(id.value(), athleteId.value())
				.map(ExerciseSubstitutionRelationshipPersistenceMapper::toDomain);
	}

	@Override
	public Optional<ExerciseSubstitutionRelationship> findActiveById(ExerciseSubstitutionRelationshipId id) {
		return jpaRepository
				.findByIdAndActiveTrue(id.value())
				.map(ExerciseSubstitutionRelationshipPersistenceMapper::toDomain);
	}

	@Override
	public java.util.List<ExerciseSubstitutionRelationship> findActiveBySourceDefinitionId(
			ExerciseDefinitionId sourceDefinitionId,
			AthleteId athleteId) {
		return jpaRepository
				.findActiveBySourceDefinitionId(sourceDefinitionId.value(), athleteId.value())
				.stream()
				.map(ExerciseSubstitutionRelationshipPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public java.util.List<ExerciseSubstitutionRelationship> findActiveBySourceDefinitionIds(
			java.util.Collection<ExerciseDefinitionId> sourceDefinitionIds,
			AthleteId athleteId) {
		if (sourceDefinitionIds == null || sourceDefinitionIds.isEmpty()) {
			return java.util.List.of();
		}
		java.util.List<java.util.UUID> ids = sourceDefinitionIds.stream()
				.map(ExerciseDefinitionId::value)
				.distinct()
				.toList();
		return jpaRepository
				.findActiveBySourceDefinitionIds(ids, athleteId.value())
				.stream()
				.map(ExerciseSubstitutionRelationshipPersistenceMapper::toDomain)
				.toList();
	}

}
