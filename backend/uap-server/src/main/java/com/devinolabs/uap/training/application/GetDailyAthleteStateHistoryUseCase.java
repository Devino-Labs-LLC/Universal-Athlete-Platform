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

@Service
public class GetDailyAthleteStateHistoryUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;

	public GetDailyAthleteStateHistoryUseCase(
			AthleteContextPort athleteContextPort,
			DailyAthleteStateSnapshotRepository snapshotRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
	}

	@Transactional(readOnly = true)
	public DailyAthleteStateHistoryPage execute(
			AccountId accountId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentOnly,
			Integer page,
			Integer size) {
		DailyAthleteStateSupport.requireHistoryRange(startDate, endDate);
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		int pageNumber = RecoveryCheckInSupport.normalizePage(page);
		int pageSize = RecoveryCheckInSupport.normalizeSize(size);
		List<DailyAthleteStateSnapshotSummary> content = snapshotRepository.findHistory(
				athleteId, startDate, endDate, currentOnly, pageNumber, pageSize);
		long total = snapshotRepository.countHistory(athleteId, startDate, endDate, currentOnly);
		return new DailyAthleteStateHistoryPage(content, pageNumber, pageSize, total);
	}

}
