package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DuplicateTrainingEnvironmentException;
import com.devinolabs.uap.training.application.TrainingEnvironmentFilters;
import com.devinolabs.uap.training.application.TrainingEnvironmentPage;
import com.devinolabs.uap.training.application.TrainingEnvironmentRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironment;
import com.devinolabs.uap.training.domain.TrainingEnvironmentId;

@Repository
class JpaTrainingEnvironmentRepository implements TrainingEnvironmentRepository {

	private final TrainingEnvironmentJpaRepository jpaRepository;

	JpaTrainingEnvironmentRepository(TrainingEnvironmentJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public TrainingEnvironment save(TrainingEnvironment environment) {
		try {
			boolean isNew = !jpaRepository.existsById(environment.id().value());
			TrainingEnvironmentJpaEntity saved = jpaRepository.save(
					TrainingEnvironmentPersistenceMapper.toEntity(environment, isNew));
			jpaRepository.flush();
			return TrainingEnvironmentPersistenceMapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw new DuplicateTrainingEnvironmentException();
		}
	}

	@Override
	public Optional<TrainingEnvironment> findById(TrainingEnvironmentId id) {
		return jpaRepository.findById(id.value()).map(TrainingEnvironmentPersistenceMapper::toDomain);
	}

	@Override
	public Optional<TrainingEnvironment> findOwnedById(TrainingEnvironmentId id, AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(TrainingEnvironmentPersistenceMapper::toDomain);
	}

	@Override
	public Optional<TrainingEnvironment> findActiveDefaultByAthleteId(AthleteId athleteId) {
		return jpaRepository.findByAthleteIdAndDefaultEnvironmentTrueAndActiveTrue(athleteId.value())
				.map(TrainingEnvironmentPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsActiveByAthleteIdAndNormalizedName(AthleteId athleteId, String normalizedName) {
		return jpaRepository.countActiveByAthleteIdAndNormalizedName(athleteId.value(), normalizedName, null) > 0;
	}

	@Override
	public boolean existsActiveByAthleteIdAndNormalizedNameExcluding(
			AthleteId athleteId,
			String normalizedName,
			TrainingEnvironmentId excludingId) {
		return jpaRepository.countActiveByAthleteIdAndNormalizedName(
				athleteId.value(), normalizedName, excludingId.value()) > 0;
	}

	@Override
	public boolean hasAnyActiveByAthleteId(AthleteId athleteId) {
		return jpaRepository.existsActiveByAthleteId(athleteId.value());
	}

	@Override
	public Optional<TrainingEnvironment> findActiveDefaultForUpdate(AthleteId athleteId) {
		return findActiveDefaultByAthleteId(athleteId);
	}

	@Override
	public void clearDefaultForAthleteExcept(AthleteId athleteId, TrainingEnvironmentId keepDefaultId) {
		jpaRepository.clearDefaultForAthleteExcept(
				athleteId.value(), keepDefaultId == null ? null : keepDefaultId.value());
	}

	@Override
	public TrainingEnvironmentPage findByAthlete(
			AthleteId athleteId,
			TrainingEnvironmentFilters filters,
			int page,
			int size) {
		Page<TrainingEnvironmentJpaEntity> found = jpaRepository.findFiltered(
				athleteId.value(),
				filters.type(),
				filters.equipment().size(),
				filters.activeOnly(),
				PageRequest.of(page, size));
		List<TrainingEnvironment> environments = found.getContent().stream()
				.map(TrainingEnvironmentPersistenceMapper::toDomain)
				.filter(environment -> matchesEquipment(environment, filters.equipment()))
				.toList();
		return new TrainingEnvironmentPage(environments, page, size, found.getTotalElements());
	}

	@Override
	public List<TrainingEnvironment> findAllActiveByAthleteId(AthleteId athleteId) {
		return jpaRepository.findByAthleteIdAndActiveTrueOrderByDefaultEnvironmentDescNameAscIdAsc(athleteId.value())
				.stream()
				.map(TrainingEnvironmentPersistenceMapper::toDomain)
				.toList();
	}

	private static boolean matchesEquipment(TrainingEnvironment environment, List<EquipmentType> required) {
		if (required.isEmpty()) {
			return true;
		}
		return environment.availableEquipment().containsAll(required);
	}

}
