package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.TrainingPlanId;
import com.devinolabs.uap.training.domain.WorkoutOccurrenceStatus;

@Service
public class GetAthleteTrainingCalendarUseCase {

	private final AthleteContextPort athleteContextPort;
	private final AthleteTrainingCalendarReader calendarReader;

	public GetAthleteTrainingCalendarUseCase(
			AthleteContextPort athleteContextPort,
			AthleteTrainingCalendarReader calendarReader) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.calendarReader = Objects.requireNonNull(calendarReader);
	}

	@Transactional(readOnly = true)
	public List<AthleteCalendarEntryResult> execute(
			AccountId accountId,
			LocalDate from,
			LocalDate to,
			WorkoutOccurrenceStatus status,
			TrainingPlanId trainingPlanId) {
		AthleteTrainingCalendarReader.requireValidRange(from, to);
		AthleteRef athlete = TrainingPlanSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		return calendarReader.read(athleteId, from, to, status, trainingPlanId);
	}

}
