package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;
import com.devinolabs.uap.training.domain.RecoveryMetricType;

@Service
public class GetRecoveryMetricTrendUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;

	public GetRecoveryMetricTrendUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository,
			TrainingLoadQueryRepository trainingLoadQueryRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
		this.trainingLoadQueryRepository = Objects.requireNonNull(trainingLoadQueryRepository);
	}

	@Transactional(readOnly = true)
	public RecoveryMetricTrendResult execute(
			AccountId accountId,
			RecoveryMetricType metricType,
			LocalDate startDate,
			LocalDate endDate,
			boolean includeTrainingLoad) {
		RecoveryAnalyticsSupport.requireTrendDateRange(startDate, endDate);
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		List<DailyRecoveryCheckIn> checkIns = RecoveryAnalyticsSupport.loadCheckInsInRange(
				checkInRepository, athleteId, startDate, endDate);
		Map<LocalDate, DailyTrainingLoadSummary> loadByDate = includeTrainingLoad
				? RecoveryAnalyticsSupport.loadTrainingLoadByDate(
						trainingLoadQueryRepository, athleteId, startDate, endDate)
				: Map.of();
		return RecoveryAnalyticsSupport.buildMetricTrend(
				metricType, startDate, endDate, checkIns, loadByDate, includeTrainingLoad);
	}

}
