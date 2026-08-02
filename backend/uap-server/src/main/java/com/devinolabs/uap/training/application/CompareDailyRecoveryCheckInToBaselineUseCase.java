package com.devinolabs.uap.training.application;

import java.time.Clock;
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
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.DailyTrainingLoadSummary;

@Service
public class CompareDailyRecoveryCheckInToBaselineUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;
	private final Clock clock;

	public CompareDailyRecoveryCheckInToBaselineUseCase(
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
			DailyRecoveryCheckInId checkInId,
			int baselineWindowDays,
			boolean includeTrainingLoad) {
		RecoveryAnalyticsSupport.requireBaselineWindow(baselineWindowDays);
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyRecoveryCheckIn checkIn = checkInRepository.findByIdAndAthleteId(checkInId, athleteId)
				.orElseThrow(RecoveryCheckInNotFoundException::new);
		LocalDate targetDate = checkIn.checkInDate();
		RecoveryAnalyticsSupport.requireTargetDate(targetDate, clock);
		List<DailyRecoveryCheckIn> priorCheckIns = RecoveryAnalyticsSupport.loadPriorCheckIns(
				checkInRepository, athleteId, targetDate, baselineWindowDays);
		List<RecoveryMetricDeviationResult> deviations = RecoveryAnalyticsSupport.buildDeviations(
				checkIn, targetDate, baselineWindowDays, priorCheckIns, clock);
		RecoveryTrainingLoadContextResult load = includeTrainingLoad
				? loadContext(athleteId, targetDate)
				: null;
		return new DailyRecoveryBaselineComparisonResult(
				checkIn.id().value(),
				targetDate,
				true,
				baselineWindowDays,
				deviations,
				checkIn.discomfortAreas(),
				load,
				RecoveryAnalyticsSupport.calculatedAt(clock));
	}

	private RecoveryTrainingLoadContextResult loadContext(AthleteId athleteId, LocalDate targetDate) {
		Map<LocalDate, DailyTrainingLoadSummary> loadByDate = RecoveryAnalyticsSupport.loadTrainingLoadByDate(
				trainingLoadQueryRepository, athleteId, targetDate, targetDate);
		return RecoveryCheckInSupport.loadContextForDate(targetDate, loadByDate);
	}

}
