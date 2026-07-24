package com.devinolabs.uap.athlete.application;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;

@Service
public class ListCurrentAthleteMeasurementsUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteMeasurementRepository measurementRepository;

	public ListCurrentAthleteMeasurementsUseCase(
			AthleteRepository athleteRepository,
			AthleteMeasurementRepository measurementRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.measurementRepository = Objects.requireNonNull(measurementRepository);
	}

	@Transactional(readOnly = true)
	public List<AthleteMeasurementResult> execute(
			AccountId accountId,
			MeasurementType measurementType,
			MeasurementSource source,
			UUID athleteSportId,
			UUID athleteGoalId,
			Instant measuredFrom,
			Instant measuredTo) {
		if (measuredFrom != null && measuredTo != null && measuredFrom.isAfter(measuredTo)) {
			throw new InvalidMeasurementDateRangeException("measuredFrom must not be after measuredTo");
		}
		Athlete athlete = AthleteMeasurementSupport.requireAthlete(athleteRepository, accountId);
		return AthleteMeasurementSupport.ordered(measurementRepository.findFiltered(
				athlete.id(),
				measurementType,
				source,
				athleteSportId,
				athleteGoalId,
				measuredFrom,
				measuredTo));
	}

}
