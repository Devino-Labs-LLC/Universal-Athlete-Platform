package com.devinolabs.uap.athlete.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteMeasurement;
import com.devinolabs.uap.athlete.domain.AthleteMeasurementId;

@Service
public class DeleteAthleteMeasurementUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteMeasurementRepository measurementRepository;

	public DeleteAthleteMeasurementUseCase(
			AthleteRepository athleteRepository,
			AthleteMeasurementRepository measurementRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.measurementRepository = Objects.requireNonNull(measurementRepository);
	}

	@Transactional
	public void execute(AccountId accountId, AthleteMeasurementId measurementId) {
		Athlete athlete = AthleteMeasurementSupport.requireMutableAthlete(athleteRepository, accountId);
		AthleteMeasurement measurement = measurementRepository.findByIdAndAthleteId(measurementId, athlete.id())
				.orElseThrow(AthleteMeasurementNotFoundException::new);
		measurementRepository.delete(measurement);
	}

}
