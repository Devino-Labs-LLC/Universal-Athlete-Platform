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
public class GetRecoveryBaselineDashboardUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;
	private final Clock clock;

	public GetRecoveryBaselineDashboardUseCase(
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
	public RecoveryBaselineDashboardResult execute(
			AccountId accountId,
			LocalDate targetDate,
			int baselineWindowDays,
			boolean includeTrainingLoad) {
		RecoveryAnalyticsSupport.requireBaselineWindow(baselineWindowDays);
		LocalDate resolvedTargetDate = targetDate == null ? LocalDate.now(clock) : targetDate;
		RecoveryAnalyticsSupport.requireTargetDate(resolvedTargetDate, clock);
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		List<DailyRecoveryCheckIn> priorCheckIns = RecoveryAnalyticsSupport.loadPriorCheckIns(
				checkInRepository, athleteId, resolvedTargetDate, baselineWindowDays);
		List<RecoveryMetricBaselineResult> baselines = RecoveryAnalyticsSupport.buildBaselines(
				resolvedTargetDate, baselineWindowDays, priorCheckIns, clock);
		Optional<DailyRecoveryCheckIn> checkIn = checkInRepository.findByAthleteIdAndCheckInDate(
				athleteId, resolvedTargetDate);
		List<RecoveryMetricDeviationResult> deviations = checkIn
				.map(target -> RecoveryAnalyticsSupport.buildDeviations(
						target, resolvedTargetDate, baselineWindowDays, priorCheckIns, clock))
				.orElse(List.of());
		LocalDate trendStart = resolvedTargetDate.minusDays(baselineWindowDays);
		List<DailyRecoveryCheckIn> trendCheckIns = RecoveryAnalyticsSupport.loadCheckInsInRange(
				checkInRepository, athleteId, trendStart, resolvedTargetDate);
		List<RecoveryMetricDashboardTrendResult> metricTrends = RecoveryAnalyticsSupport.buildDashboardTrends(
				resolvedTargetDate, baselineWindowDays, trendCheckIns);
		RecoveryTrainingLoadContextResult load = includeTrainingLoad
				? loadContext(athleteId, resolvedTargetDate)
				: null;
		return new RecoveryBaselineDashboardResult(
				resolvedTargetDate,
				checkIn.isPresent(),
				checkIn.map(DailyRecoveryCheckInResult::from).orElse(null),
				baselineWindowDays,
				baselines,
				deviations,
				metricTrends,
				load,
				RecoveryAnalyticsSupport.calculatedAt(clock));
	}

	private RecoveryTrainingLoadContextResult loadContext(AthleteId athleteId, LocalDate targetDate) {
		Map<LocalDate, DailyTrainingLoadSummary> loadByDate = RecoveryAnalyticsSupport.loadTrainingLoadByDate(
				trainingLoadQueryRepository, athleteId, targetDate, targetDate);
		return RecoveryCheckInSupport.loadContextForDate(targetDate, loadByDate);
	}

}
