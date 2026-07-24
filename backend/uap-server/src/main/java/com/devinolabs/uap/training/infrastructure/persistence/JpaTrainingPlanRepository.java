package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.TrainingPlanRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlan;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;

@Repository
class JpaTrainingPlanRepository implements TrainingPlanRepository {

	private final TrainingPlanJpaRepository jpaRepository;

	JpaTrainingPlanRepository(TrainingPlanJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public TrainingPlan save(TrainingPlan plan) {
		boolean isNew = !jpaRepository.existsById(plan.id().value());
		TrainingPlanJpaEntity saved = jpaRepository.save(TrainingPlanPersistenceMapper.toEntity(plan, isNew));
		return TrainingPlanPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<TrainingPlan> findByIdAndAthleteId(TrainingPlanId id, AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(TrainingPlanPersistenceMapper::toDomain);
	}

	@Override
	public List<TrainingPlan> findAllByAthleteId(AthleteId athleteId) {
		return jpaRepository.findAllByAthleteIdOrderByStartDateDescCreatedAtDescIdAsc(athleteId.value())
				.stream()
				.map(TrainingPlanPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public List<TrainingPlan> findFiltered(AthleteId athleteId, TrainingPlanStatus status, TrainingPlanType planType) {
		return jpaRepository.findFiltered(athleteId.value(), status, planType)
				.stream()
				.map(TrainingPlanPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsOverlappingDuplicate(
			AthleteId athleteId,
			String normalizedName,
			LocalDate startDate,
			LocalDate endDate,
			TrainingPlanId excludingId) {
		return jpaRepository.existsOverlappingDuplicate(
				athleteId.value(),
				normalizedName,
				startDate,
				endDate,
				excludingId == null ? null : excludingId.value());
	}

	@Override
	public void delete(TrainingPlan plan) {
		jpaRepository.deleteById(plan.id().value());
	}

}
