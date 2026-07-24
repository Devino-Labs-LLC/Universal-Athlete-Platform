package com.devinolabs.uap.athlete.application;

import java.util.Comparator;
import java.util.List;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteGoalId;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteSportId;
import com.devinolabs.uap.athlete.domain.AthleteStatus;

final class AthleteMeasurementSupport {

	private AthleteMeasurementSupport() {
	}

	static Athlete requireMutableAthlete(AthleteRepository athleteRepository, AccountId accountId) {
		Athlete athlete = athleteRepository.findByAccountId(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);
		if (athlete.status() == AthleteStatus.ARCHIVED) {
			throw new AthleteArchivedException();
		}
		return athlete;
	}

	static Athlete requireAthlete(AthleteRepository athleteRepository, AccountId accountId) {
		return athleteRepository.findByAccountId(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);
	}

	static void assertLinkedSportBelongsToAthlete(
			AthleteSportRepository sportRepository,
			Athlete athlete,
			AthleteSportId athleteSportId) {
		if (athleteSportId == null) {
			return;
		}
		sportRepository.findByIdAndAthleteId(athleteSportId, athlete.id())
				.orElseThrow(AthleteSportNotFoundException::new);
	}

	static void assertLinkedGoalBelongsToAthlete(
			AthleteGoalRepository goalRepository,
			Athlete athlete,
			AthleteGoalId athleteGoalId) {
		if (athleteGoalId == null) {
			return;
		}
		goalRepository.findByIdAndAthleteId(athleteGoalId, athlete.id())
				.orElseThrow(AthleteGoalNotFoundException::new);
	}

	static AthleteMeasurementResult toResult(AthleteMeasurement measurement) {
		return new AthleteMeasurementResult(
				measurement.id(),
				measurement.measurementType(),
				measurement.customMeasurementName(),
				measurement.value(),
				measurement.unit(),
				measurement.customUnit(),
				measurement.source(),
				measurement.notes(),
				measurement.measuredAt(),
				measurement.athleteSportId(),
				measurement.athleteGoalId(),
				measurement.createdAt(),
				measurement.updatedAt());
	}

	static List<AthleteMeasurementResult> ordered(List<AthleteMeasurement> measurements) {
		return measurements.stream()
				.sorted(Comparator
						.comparing(AthleteMeasurement::measuredAt).reversed()
						.thenComparing(AthleteMeasurement::createdAt, Comparator.reverseOrder())
						.thenComparing(measurement -> measurement.id().value()))
				.map(AthleteMeasurementSupport::toResult)
				.toList();
	}

	static RuntimeException translateValidation(IllegalArgumentException ex) {
		String message = ex.getMessage() == null ? "" : ex.getMessage();
		if (message.contains("incompatible")) {
			return new InvalidMeasurementTypeUnitCombinationException(message);
		}
		if (message.contains("customMeasurementName")) {
			return new InvalidCustomMeasurementNameException(message);
		}
		if (message.contains("customUnit")) {
			return new InvalidCustomMeasurementUnitException(message);
		}
		if (message.contains("measuredAt")) {
			return new InvalidMeasurementTimestampException(message);
		}
		if (message.contains("unit")) {
			return new InvalidMeasurementUnitException(message);
		}
		if (message.contains("value")
				|| message.contains("body fat")
				|| message.contains("SESSION_RPE")
				|| message.contains("DECIMAL")) {
			return new InvalidMeasurementValueException(message);
		}
		return ex;
	}

}
