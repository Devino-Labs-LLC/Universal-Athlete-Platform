package com.devinolabs.uap.athlete.infrastructure.persistence;

import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;

final class AthleteMeasurementPersistenceMapper {

	private AthleteMeasurementPersistenceMapper() {
	}

	static AthleteMeasurementJpaEntity toEntity(AthleteMeasurement measurement, boolean isNew) {
		return new AthleteMeasurementJpaEntity(
				measurement.id().value(),
				measurement.athleteId().value(),
				measurement.measurementType(),
				measurement.customMeasurementName(),
				measurement.value(),
				measurement.unit(),
				measurement.customUnit(),
				measurement.source(),
				measurement.notes(),
				measurement.measuredAt(),
				measurement.athleteSportId() == null ? null : measurement.athleteSportId().value(),
				measurement.athleteGoalId() == null ? null : measurement.athleteGoalId().value(),
				measurement.createdAt(),
				measurement.updatedAt(),
				measurement.version(),
				isNew);
	}

	static AthleteMeasurement toDomain(AthleteMeasurementJpaEntity entity) {
		return AthleteMeasurement.rehydrate(
				AthleteMeasurementId.of(entity.getId()),
				AthleteId.of(entity.getAthleteId()),
				entity.getMeasurementType(),
				entity.getCustomMeasurementName(),
				entity.getMeasurementValue(),
				entity.getMeasurementUnit(),
				entity.getCustomUnit(),
				entity.getSource(),
				entity.getNotes(),
				entity.getMeasuredAt(),
				entity.getAthleteSportId() == null ? null : AthleteSportId.of(entity.getAthleteSportId()),
				entity.getAthleteGoalId() == null ? null : AthleteGoalId.of(entity.getAthleteGoalId()),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
