package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;

/**
 * Athletes carry no stored timezone, so callers must supply the zone that "today" should be
 * resolved in; the current instant comes from the injected clock.
 */
@Service
public class GetAthleteTrainingTodayUseCase {

	private final AthleteContextPort athleteContextPort;
	private final AthleteTrainingCalendarReader calendarReader;
	private final Clock clock;

	public GetAthleteTrainingTodayUseCase(
			AthleteContextPort athleteContextPort,
			AthleteTrainingCalendarReader calendarReader,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.calendarReader = Objects.requireNonNull(calendarReader);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public AthleteTrainingTodayResult execute(AccountId accountId, String timezone) {
		ZoneId zone = TrainingScheduleSupport.requireZone(timezone);
		AthleteRef athlete = TrainingPlanSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		LocalDate today = LocalDate.now(clock.withZone(zone));
		return new AthleteTrainingTodayResult(
				today,
				zone.getId(),
				calendarReader.read(athleteId, today, today, null, null));
	}

}
