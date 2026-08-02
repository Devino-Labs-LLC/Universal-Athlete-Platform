package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;

@Service
public class CompareRecoveryDateToBaselineUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;
	private final Clock clock;

	public CompareRecoveryDateToBaselineUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository,
			TrainingLoadQueryRepository trainingLoadQueryRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
		this.trainingLoadQueryRepository = Objects.requireNonNull(trainingLoadQueryRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public DailyRecoveryBaselineComparisonResult execute(
			AccountId accountId,
			LocalDate date,
			int baselineWindowDays,
			boolean includeTrainingLoad) {
		RecoveryAnalyticsSupport.requireBaselineWindow(baselineWindowDays);
		RecoveryAnalyticsSupport.requireTargetDate(date, clock);
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		List<DailyRecoveryCheckIn> priorCheckIns = RecoveryAnalyticsSupport.loadPriorCheckIns(
				checkInRepository, athleteId, date, baselineWindowDays);
		Optional<DailyRecoveryCheckIn> checkIn = checkInRepository.findByAthleteIdAndCheckInDate(athleteId, date);
		if (checkIn.isEmpty()) {
			return new DailyRecoveryBaselineComparisonResult(
					null,
					date,
					false,
					baselineWindowDays,
					RecoveryAnalyticsSupport.unavailableMetricComparisons(
							date, baselineWindowDays, priorCheckIns, clock),
					List.of(),
					includeTrainingLoad ? loadContext(athleteId, date) : null,
					RecoveryAnalyticsSupport.calculatedAt(clock));
		}
		DailyRecoveryCheckIn targetCheckIn = checkIn.get();
		List<RecoveryMetricDeviationResult> deviations = RecoveryAnalyticsSupport.buildDeviations(
				targetCheckIn, date, baselineWindowDays, priorCheckIns, clock);
		return new DailyRecoveryBaselineComparisonResult(
				targetCheckIn.id().value(),
				date,
				true,
				baselineWindowDays,
				deviations,
				targetCheckIn.discomfortAreas(),
				includeTrainingLoad ? loadContext(athleteId, date) : null,
				RecoveryAnalyticsSupport.calculatedAt(clock));
	}

	private RecoveryTrainingLoadContextResult loadContext(AthleteId athleteId, LocalDate targetDate) {
		Map<LocalDate, DailyTrainingLoadSummary> loadByDate = RecoveryAnalyticsSupport.loadTrainingLoadByDate(
				trainingLoadQueryRepository, athleteId, targetDate, targetDate);
		return RecoveryCheckInSupport.loadContextForDate(targetDate, loadByDate);
	}

}
