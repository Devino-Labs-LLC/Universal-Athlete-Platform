package com.devinolabs.uap.athlete.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.MeasurementUnit;

@Service
public class UpdateAthleteMeasurementUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteMeasurementRepository measurementRepository;
	private final AthleteSportRepository athleteSportRepository;
	private final AthleteGoalRepository athleteGoalRepository;
	private final Clock clock;

	public UpdateAthleteMeasurementUseCase(
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
			AthleteMeasurementId measurementId,
			UpdateAthleteMeasurementCommand command) {
		Athlete athlete = AthleteMeasurementSupport.requireMutableAthlete(athleteRepository, accountId);
		AthleteMeasurement measurement = measurementRepository.findByIdAndAthleteId(measurementId, athlete.id())
				.orElseThrow(AthleteMeasurementNotFoundException::new);

		if (command.athleteSportIdPresent()) {
			AthleteSportId sportId = command.athleteSportId() == null
					? null
					: AthleteSportId.of(command.athleteSportId());
			AthleteMeasurementSupport.assertLinkedSportBelongsToAthlete(athleteSportRepository, athlete, sportId);
		}
		if (command.athleteGoalIdPresent()) {
			AthleteGoalId goalId = command.athleteGoalId() == null
					? null
					: AthleteGoalId.of(command.athleteGoalId());
			AthleteMeasurementSupport.assertLinkedGoalBelongsToAthlete(athleteGoalRepository, athlete, goalId);
		}

		try {
			boolean valueChanging = command.valuePresent();
			boolean unitChanging = command.unitPresent() || command.customUnitPresent();
			if (valueChanging || unitChanging) {
				BigDecimal value = command.valuePresent() ? command.value() : measurement.value();
				MeasurementUnit unit = command.unitPresent() ? command.unit() : measurement.unit();
				String customUnit = command.customUnitPresent() ? command.customUnit() : measurement.customUnit();
				if (value == null) {
					throw new IllegalArgumentException("value must not be null");
				}
				if (unit == null) {
					throw new IllegalArgumentException("unit must not be null");
				}
				measurement.correctValueAndUnit(value, unit, customUnit, clock);
			}
			if (command.measuredAtPresent()) {
				if (command.measuredAt() == null) {
					throw new IllegalArgumentException("measuredAt must not be null");
				}
				measurement.correctMeasuredAt(command.measuredAt(), clock);
			}
			if (command.notesPresent()) {
				measurement.updateNotes(command.notes(), clock);
			}
			if (command.athleteSportIdPresent()) {
				if (command.athleteSportId() == null) {
					measurement.unlinkSport(clock);
				}
				else {
					measurement.linkSport(AthleteSportId.of(command.athleteSportId()), clock);
				}
			}
			if (command.athleteGoalIdPresent()) {
				if (command.athleteGoalId() == null) {
					measurement.unlinkGoal(clock);
				}
				else {
					measurement.linkGoal(AthleteGoalId.of(command.athleteGoalId()), clock);
				}
			}
		}
		catch (IllegalArgumentException ex) {
			throw AthleteMeasurementSupport.translateValidation(ex);
		}

		return AthleteMeasurementSupport.toResult(measurementRepository.save(measurement));
	}

}
