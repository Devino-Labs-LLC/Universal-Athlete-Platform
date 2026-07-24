package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.athlete.application.AssessmentMeasurementRepository;
import com.devinolabs.uap.athlete.application.DuplicateAssessmentMeasurementException;
import com.devinolabs.uap.athlete.domain.AssessmentId;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurement;
import com.devinolabs.uap.athlete.domain.AssessmentMeasurementId;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;

@Repository
class JpaAssessmentMeasurementRepository implements AssessmentMeasurementRepository {

	private final AssessmentMeasurementJpaRepository jpaRepository;

	JpaAssessmentMeasurementRepository(AssessmentMeasurementJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public AssessmentMeasurement save(AssessmentMeasurement attachment) {
		try {
			boolean isNew = !jpaRepository.existsById(attachment.id().value());
			AssessmentMeasurementJpaEntity saved = jpaRepository.save(
					AssessmentMeasurementPersistenceMapper.toEntity(attachment, isNew));
			return AssessmentMeasurementPersistenceMapper.toDomain(saved);
		}
		catch (DataIntegrityViolationException ex) {
			throw mapDuplicate(ex);
		}
	}

	@Override
	public List<AssessmentMeasurement> saveAll(Collection<AssessmentMeasurement> attachments) {
		try {
			List<AssessmentMeasurementJpaEntity> entities = new ArrayList<>(attachments.size());
			for (AssessmentMeasurement attachment : attachments) {
				boolean isNew = !jpaRepository.existsById(attachment.id().value());
				entities.add(AssessmentMeasurementPersistenceMapper.toEntity(attachment, isNew));
			}
			return jpaRepository.saveAll(entities).stream()
					.map(AssessmentMeasurementPersistenceMapper::toDomain)
					.toList();
		}
		catch (DataIntegrityViolationException ex) {
			throw mapDuplicate(ex);
		}
	}

	@Override
	public Optional<AssessmentMeasurement> findByIdAndAssessmentIdAndAthleteId(
			AssessmentMeasurementId id,
			AssessmentId assessmentId,
			AthleteId athleteId) {
		return jpaRepository.findByIdAndAssessmentIdAndAthleteId(
						id.value(),
						assessmentId.value(),
						athleteId.value())
				.map(AssessmentMeasurementPersistenceMapper::toDomain);
	}

	@Override
	public List<AssessmentMeasurement> findAllByAssessmentIdAndAthleteId(
			AssessmentId assessmentId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllByAssessmentIdAndAthleteIdOrderByDisplayOrderAscCreatedAtAscIdAsc(
						assessmentId.value(),
						athleteId.value())
				.stream()
				.map(AssessmentMeasurementPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public boolean existsByAssessmentIdAndSourceMeasurementId(
			AssessmentId assessmentId,
			AthleteMeasurementId sourceMeasurementId) {
		return jpaRepository.existsByAssessmentIdAndSourceMeasurementId(
				assessmentId.value(),
				sourceMeasurementId.value());
	}

	@Override
	public boolean existsActiveAttachmentBySourceMeasurementId(AthleteMeasurementId sourceMeasurementId) {
		return jpaRepository.existsActiveAttachmentBySourceMeasurementId(sourceMeasurementId.value());
	}

	@Override
	public long countByAssessmentIdAndAthleteId(AssessmentId assessmentId, AthleteId athleteId) {
		return jpaRepository.countByAssessmentIdAndAthleteId(assessmentId.value(), athleteId.value());
	}

	@Override
	public int findMaxDisplayOrder(AssessmentId assessmentId, AthleteId athleteId) {
		return jpaRepository.findMaxDisplayOrder(assessmentId.value(), athleteId.value());
	}

	@Override
	public void delete(AssessmentMeasurement attachment) {
		jpaRepository.deleteById(attachment.id().value());
	}

	@Override
	public void deleteByIdAndAssessmentIdAndAthleteId(
			AssessmentMeasurementId id,
			AssessmentId assessmentId,
			AthleteId athleteId) {
		jpaRepository.deleteByIdAndAssessmentIdAndAthleteId(id.value(), assessmentId.value(), athleteId.value());
	}

	private static RuntimeException mapDuplicate(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
		if (message != null && message.toLowerCase().contains("uq_assessment_measurements_assessment_source")) {
			return new DuplicateAssessmentMeasurementException();
		}
		return ex;
	}

}
