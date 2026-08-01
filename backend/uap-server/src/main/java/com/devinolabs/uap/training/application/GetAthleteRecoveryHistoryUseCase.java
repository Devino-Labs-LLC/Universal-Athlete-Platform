package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.ArrayList;
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

@Service
public class GetAthleteRecoveryHistoryUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final DailyRecoveryCheckInRevisionRepository revisionRepository;
	private final TrainingLoadQueryRepository trainingLoadQueryRepository;

	public GetAthleteRecoveryHistoryUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository,
			DailyRecoveryCheckInRevisionRepository revisionRepository,
			TrainingLoadQueryRepository trainingLoadQueryRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
		this.revisionRepository = Objects.requireNonNull(revisionRepository);
		this.trainingLoadQueryRepository = Objects.requireNonNull(trainingLoadQueryRepository);
	}

	@Transactional(readOnly = true)
	public AthleteRecoveryHistoryResult execute(
			AccountId accountId,
			LocalDate startDate,
			LocalDate endDate,
			boolean includeTrainingLoad) {
		RecoveryCheckInSupport.requireListDateRange(startDate, endDate);
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());

		List<DailyRecoveryCheckIn> checkIns =
				checkInRepository.findAllByAthleteAndDateRange(athleteId, startDate, endDate);
		Map<LocalDate, DailyRecoveryCheckIn> checkInsByDate = RecoveryCheckInSupport.indexByDate(checkIns);
		Map<LocalDate, DailyTrainingLoadSummary> loadByDate = includeTrainingLoad
				? RecoveryCheckInSupport.indexLoadByDate(trainingLoadQueryRepository.aggregateDaily(
						athleteId, startDate, endDate, null, null, null, null))
				: Map.of();

		List<AthleteRecoveryHistoryDayResult> days = new ArrayList<>();
		for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
			DailyRecoveryCheckIn checkIn = checkInsByDate.get(date);
			if (checkIn == null) {
				continue;
			}
			int revisionCount = revisionRepository.countByCheckInId(checkIn.id());
			RecoveryTrainingLoadContextResult load = includeTrainingLoad
					? RecoveryCheckInSupport.loadContextForDate(date, loadByDate)
					: null;
			days.add(new AthleteRecoveryHistoryDayResult(
					date,
					DailyRecoveryCheckInResult.from(checkIn),
					load,
					revisionCount,
					checkIn.updatedAt()));
		}
		return new AthleteRecoveryHistoryResult(days);
	}

}
