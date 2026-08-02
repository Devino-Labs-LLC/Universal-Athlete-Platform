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
import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.BodySide;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;

@Service
public class GetBodyAreaDiscomfortHistoryUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;

	public GetBodyAreaDiscomfortHistoryUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
	}

	@Transactional(readOnly = true)
	public BodyAreaDiscomfortHistoryResult execute(
			AccountId accountId,
			LocalDate startDate,
			LocalDate endDate,
			BodyArea bodyArea,
			BodySide bodySide) {
		RecoveryAnalyticsSupport.requireTrendDateRange(startDate, endDate);
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		List<DailyRecoveryCheckIn> checkIns = RecoveryAnalyticsSupport.loadCheckInsInRange(
				checkInRepository, athleteId, startDate, endDate);
		return RecoveryAnalyticsSupport.buildDiscomfortHistory(
				startDate, endDate, checkIns, bodyArea, bodySide);
	}

}
