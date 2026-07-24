package com.devinolabs.uap.athlete.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;

public interface AthleteMeasurementRepository {

	AthleteMeasurement save(AthleteMeasurement measurement);

	Optional<AthleteMeasurement> findByIdAndAthleteId(AthleteMeasurementId id, AthleteId athleteId);

	List<AthleteMeasurement> findFiltered(
			AthleteId athleteId,
			MeasurementType measurementType,
			MeasurementSource source,
			UUID athleteSportId,
			UUID athleteGoalId,
			Instant measuredFrom,
			Instant measuredTo);

	void delete(AthleteMeasurement measurement);

}
