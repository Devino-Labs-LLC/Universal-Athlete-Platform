package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.ExerciseDefinitionFilters;
import com.devinolabs.uap.training.application.ExerciseDefinitionPage;
import com.devinolabs.uap.training.application.ExerciseDefinitionRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.ExerciseDefinition;
import com.devinolabs.uap.training.domain.ExerciseDefinitionId;
import com.devinolabs.uap.training.domain.ExerciseDefinitionScope;

@Repository
class JpaExerciseDefinitionRepository implements ExerciseDefinitionRepository {

	private final ExerciseDefinitionJpaRepository jpaRepository;

	JpaExerciseDefinitionRepository(ExerciseDefinitionJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public ExerciseDefinition save(ExerciseDefinition definition) {
		boolean isNew = !jpaRepository.existsById(definition.id().value());
		ExerciseDefinitionJpaEntity saved = jpaRepository.save(
				ExerciseDefinitionPersistenceMapper.toEntity(definition, isNew));
		// Flush so the unique active-name index reports a conflict inside this use case rather than
		// at an arbitrary later point in the transaction.
		jpaRepository.flush();
		return ExerciseDefinitionPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<ExerciseDefinition> findById(ExerciseDefinitionId id) {
		return jpaRepository.findById(id.value()).map(ExerciseDefinitionPersistenceMapper::toDomain);
	}

	@Override
	public Optional<ExerciseDefinition> findAccessible(ExerciseDefinitionId id, AthleteId athleteId) {
		return jpaRepository
				.findAccessible(id.value(), athleteId.value())
				.map(ExerciseDefinitionPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsActiveSystemByNormalizedName(String normalizedName) {
		return jpaRepository.countActiveSystemByNormalizedName(normalizedName) > 0;
	}

	@Override
	public boolean existsActiveCustomByAthleteIdAndNormalizedName(AthleteId athleteId, String normalizedName) {
		return jpaRepository.countActiveCustomByAthleteIdAndNormalizedName(
				athleteId.value(), normalizedName, null) > 0;
	}

	@Override
	public boolean existsActiveCustomByAthleteIdAndNormalizedNameExcluding(
			AthleteId athleteId,
			String normalizedName,
			ExerciseDefinitionId excludingId) {
		return jpaRepository.countActiveCustomByAthleteIdAndNormalizedName(
				athleteId.value(), normalizedName, excludingId.value()) > 0;
	}

	@Override
	public ExerciseDefinitionPage findAccessibleActive(
			AthleteId athleteId,
			ExerciseDefinitionFilters filters,
			int page,
			int size) {
		Page<ExerciseDefinitionJpaEntity> found = jpaRepository.findAccessibleActive(
				athleteId.value(),
				filters.nameContains(),
				filters.scope(),
				filters.category(),
				filters.metricMode(),
				filters.movementPattern(),
				filters.muscleGroup(),
				filters.equipment(),
				filters.laterality(),
				filters.impactLevel(),
				filters.difficulty(),
				PageRequest.of(page, size));
		return new ExerciseDefinitionPage(
				found.getContent().stream().map(ExerciseDefinitionPersistenceMapper::toDomain).toList(),
				page,
				size,
				found.getTotalElements());
	}

	@Override
	public List<ExerciseDefinition> findAllByIds(List<ExerciseDefinitionId> ids) {
		if (ids.isEmpty()) {
			return List.of();
		}
		return jpaRepository
				.findAllByIdIn(ids.stream().map(ExerciseDefinitionId::value).toList())
				.stream()
				.map(ExerciseDefinitionPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public Map<ExerciseDefinitionId, Set<EquipmentType>> findAccessibleActiveRequiredEquipmentByIds(
			List<ExerciseDefinitionId> ids,
			AthleteId athleteId) {
		if (ids == null || ids.isEmpty()) {
			return Map.of();
		}
		List<UUID> rawIds = ids.stream().map(ExerciseDefinitionId::value).distinct().toList();
		Map<ExerciseDefinitionId, Set<EquipmentType>> byId = new LinkedHashMap<>();
		for (Object[] row : jpaRepository.findAccessibleActiveRequiredEquipmentRows(rawIds, athleteId.value())) {
			ExerciseDefinitionId definitionId = ExerciseDefinitionId.of((UUID) row[0]);
			Set<EquipmentType> equipment = byId.computeIfAbsent(
					definitionId, ignored -> EnumSet.noneOf(EquipmentType.class));
			if (row[1] instanceof EquipmentType type) {
				equipment.add(type);
			}
		}
		return byId;
	}

}
