package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.athlete.application.AthleteMeasurementRepository;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;

@Repository
class JpaAthleteMeasurementRepository implements AthleteMeasurementRepository {

	private final AthleteMeasurementJpaRepository jpaRepository;

	JpaAthleteMeasurementRepository(AthleteMeasurementJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public AthleteMeasurement save(AthleteMeasurement measurement) {
		boolean isNew = !jpaRepository.existsById(measurement.id().value());
		AthleteMeasurementJpaEntity saved = jpaRepository.save(
				AthleteMeasurementPersistenceMapper.toEntity(measurement, isNew));
		return AthleteMeasurementPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<AthleteMeasurement> findByIdAndAthleteId(AthleteMeasurementId id, AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(AthleteMeasurementPersistenceMapper::toDomain);
	}

	@Override
	public List<AthleteMeasurement> findFiltered(
			AthleteId athleteId,
			MeasurementType measurementType,
			MeasurementSource source,
			UUID athleteSportId,
			UUID athleteGoalId,
			Instant measuredFrom,
			Instant measuredTo) {
		return jpaRepository.findFiltered(
						athleteId.value(),
						measurementType,
						source,
						athleteSportId,
						athleteGoalId,
						measuredFrom,
						measuredTo)
				.stream()
				.map(AthleteMeasurementPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public void delete(AthleteMeasurement measurement) {
		jpaRepository.deleteById(measurement.id().value());
	}

}
