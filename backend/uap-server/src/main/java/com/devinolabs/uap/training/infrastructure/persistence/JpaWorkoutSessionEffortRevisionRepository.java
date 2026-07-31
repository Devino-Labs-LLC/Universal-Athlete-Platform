package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.WorkoutSessionEffortRevisionRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortId;
import com.devinolabs.uap.training.domain.WorkoutSessionEffortRevision;

@Repository
class JpaWorkoutSessionEffortRevisionRepository implements WorkoutSessionEffortRevisionRepository {

	private final WorkoutSessionEffortRevisionJpaRepository jpaRepository;

	JpaWorkoutSessionEffortRevisionRepository(WorkoutSessionEffortRevisionJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public WorkoutSessionEffortRevision save(WorkoutSessionEffortRevision revision) {
		WorkoutSessionEffortRevisionJpaEntity saved = jpaRepository
				.save(WorkoutSessionEffortRevisionPersistenceMapper.toEntity(revision));
		return WorkoutSessionEffortRevisionPersistenceMapper.toDomain(saved);
	}

	@Override
	public List<WorkoutSessionEffortRevision> findAllByEffortIdAndAthleteIdOrderByRevisionNumber(
			WorkoutSessionEffortId effortId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllByEffortIdAndAthleteIdOrderByRevisionNumber(effortId.value(), athleteId.value())
				.stream()
				.map(WorkoutSessionEffortRevisionPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public int countByEffortId(WorkoutSessionEffortId effortId) {
		return jpaRepository.countByWorkoutSessionEffortId(effortId.value());
	}

}
