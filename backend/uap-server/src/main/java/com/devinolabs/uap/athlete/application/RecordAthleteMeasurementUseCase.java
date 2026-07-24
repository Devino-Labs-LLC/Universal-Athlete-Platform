package com.devinolabs.uap.athlete.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;

@Service
public class RecordAthleteMeasurementUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteMeasurementRepository measurementRepository;
	private final AthleteSportRepository athleteSportRepository;
	private final AthleteGoalRepository athleteGoalRepository;
	private final Clock clock;

	public RecordAthleteMeasurementUseCase(
			AthleteRepository athleteRepository,
			AthleteMeasurementRepository measurementRepository,
			AthleteSportRepository athleteSportRepository,
			AthleteGoalRepository athleteGoalRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.measurementRepository = Objects.requireNonNull(measurementRepository);
		this.athleteSportRepository = Objects.requireNonNull(athleteSportRepository);
		this.athleteGoalRepository = Objects.requireNonNull(athleteGoalRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AthleteMeasurementResult execute(
			AccountId accountId,
			MeasurementType measurementType,
			String customMeasurementName,
			BigDecimal value,
			MeasurementUnit unit,
			String customUnit,
			MeasurementSource source,
			String notes,
			Instant measuredAt,
			UUID athleteSportId,
			UUID athleteGoalId) {
		Athlete athlete = AthleteMeasurementSupport.requireMutableAthlete(athleteRepository, accountId);
		AthleteSportId sportId = athleteSportId == null ? null : AthleteSportId.of(athleteSportId);
		AthleteGoalId goalId = athleteGoalId == null ? null : AthleteGoalId.of(athleteGoalId);
		AthleteMeasurementSupport.assertLinkedSportBelongsToAthlete(athleteSportRepository, athlete, sportId);
		AthleteMeasurementSupport.assertLinkedGoalBelongsToAthlete(athleteGoalRepository, athlete, goalId);

		try {
			AthleteMeasurement measurement = AthleteMeasurement.record(
					AthleteMeasurementId.generate(),
					athlete.id(),
					measurementType,
					customMeasurementName,
					value,
					unit,
					customUnit,
					source,
					notes,
					measuredAt,
					sportId,
					goalId,
					clock);
			return AthleteMeasurementSupport.toResult(measurementRepository.save(measurement));
		}
		catch (IllegalArgumentException ex) {
			throw AthleteMeasurementSupport.translateValidation(ex);
		}
	}

}
