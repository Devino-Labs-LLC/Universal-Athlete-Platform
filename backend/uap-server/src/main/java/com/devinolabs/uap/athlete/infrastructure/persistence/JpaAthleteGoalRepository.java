package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.athlete.application.AthleteGoalRepository;
import com.devinolabs.uap.athlete.domain.AthleteGoal;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.GoalStatus;
import com.devinolabs.uap.athlete.domain.GoalType;

@Repository
class JpaAthleteGoalRepository implements AthleteGoalRepository {

	private static final EnumSet<GoalStatus> ACTIVE_DUPLICATE_STATUSES = EnumSet.of(
			GoalStatus.ACTIVE,
			GoalStatus.PAUSED);

	private final AthleteGoalJpaRepository jpaRepository;

	JpaAthleteGoalRepository(AthleteGoalJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public AthleteGoal save(AthleteGoal goal) {
		boolean isNew = !jpaRepository.existsById(goal.id().value());
		AthleteGoalJpaEntity saved = jpaRepository.save(AthleteGoalPersistenceMapper.toEntity(goal, isNew));
		return AthleteGoalPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<AthleteGoal> findByIdAndAthleteId(AthleteGoalId id, AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(AthleteGoalPersistenceMapper::toDomain);
	}

	@Override
	public List<AthleteGoal> findByAthleteId(AthleteId athleteId, GoalStatus status, GoalType goalType) {
		return jpaRepository.findFiltered(athleteId.value(), status, goalType).stream()
				.map(AthleteGoalPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsActiveDuplicate(
			AthleteId athleteId,
			GoalType goalType,
			String normalizedTitle,
			AthleteGoalId excludingId) {
		if (excludingId == null) {
			return jpaRepository.existsByAthleteIdAndGoalTypeAndNormalizedTitleAndStatusIn(
					athleteId.value(),
					goalType,
					normalizedTitle,
					ACTIVE_DUPLICATE_STATUSES);
		}
		return jpaRepository.existsByAthleteIdAndGoalTypeAndNormalizedTitleAndStatusInAndIdNot(
				athleteId.value(),
				goalType,
				normalizedTitle,
				ACTIVE_DUPLICATE_STATUSES,
				excludingId.value());
	}

	@Override
	public void delete(AthleteGoal goal) {
		jpaRepository.deleteById(goal.id().value());
	}

}
