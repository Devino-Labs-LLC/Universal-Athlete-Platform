package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.athlete.application.AssessmentRepository;
import com.devinolabs.uap.athlete.domain.Assessment;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentStatus;
import com.devinolabs.uap.athlete.domain.AssessmentType;
import com.devinolabs.uap.athlete.domain.AthleteId;

@Repository
class JpaAssessmentRepository implements AssessmentRepository {

	private final AssessmentJpaRepository jpaRepository;

	JpaAssessmentRepository(AssessmentJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public Assessment save(Assessment assessment) {
		boolean isNew = !jpaRepository.existsById(assessment.id().value());
		AssessmentJpaEntity saved = jpaRepository.save(AssessmentPersistenceMapper.toEntity(assessment, isNew));
		return AssessmentPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<Assessment> findByIdAndAthleteId(AssessmentId id, AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(AssessmentPersistenceMapper::toDomain);
	}

	@Override
	public List<Assessment> findFiltered(
			AthleteId athleteId,
			AssessmentStatus status,
			AssessmentType assessmentType,
			Instant scheduledFrom,
			Instant scheduledTo) {
		return jpaRepository.findFiltered(
						athleteId.value(),
						status,
						assessmentType,
						scheduledFrom,
						scheduledTo)
				.stream()
				.map(AssessmentPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsDuplicate(
			AthleteId athleteId,
			AssessmentType type,
			String normalizedTitle,
			Instant scheduledAt,
			AssessmentId excludingId) {
		return jpaRepository.existsDuplicate(
				athleteId.value(),
				type,
				normalizedTitle,
				scheduledAt,
				excludingId == null ? null : excludingId.value());
	}

	@Override
	public void delete(Assessment assessment) {
		jpaRepository.deleteById(assessment.id().value());
	}

}
