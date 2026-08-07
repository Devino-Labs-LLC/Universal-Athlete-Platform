package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.InvalidDailyReadinessDateRangeException;
import com.devinolabs.uap.training.domain.InvalidReadinessAlgorithmVersionException;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;

@Service
public class GetDailyReadinessHistoryUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyReadinessAssessmentRepository assessmentRepository;

	public GetDailyReadinessHistoryUseCase(
			AthleteContextPort athleteContextPort,
			DailyReadinessAssessmentRepository assessmentRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
	}

	@Transactional(readOnly = true)
	public DailyReadinessHistoryPage execute(
			AccountId accountId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			String algorithmVersion,
			Integer page,
			Integer size) {
		requireRange(startDate, endDate);
		ReadinessAlgorithmVersion version = parseAlgorithm(algorithmVersion);
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		int pageNumber = RecoveryCheckInSupport.normalizePage(page);
		int pageSize = RecoveryCheckInSupport.normalizeSize(size);
		List<DailyReadinessAssessmentSummary> content = assessmentRepository.findHistory(
				athleteId, startDate, endDate, currentSnapshotOnly, version, pageNumber, pageSize);
		long total = assessmentRepository.countHistory(
				athleteId, startDate, endDate, currentSnapshotOnly, version);
		return new DailyReadinessHistoryPage(content, pageNumber, pageSize, total);
	}

	private static void requireRange(LocalDate startDate, LocalDate endDate) {
		Objects.requireNonNull(startDate, "startDate must not be null");
		Objects.requireNonNull(endDate, "endDate must not be null");
		if (endDate.isBefore(startDate)) {
			throw new InvalidDailyReadinessDateRangeException("endDate must not be before startDate");
		}
		long span = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		if (span > DailyAthleteStateSupport.MAX_HISTORY_DAYS) {
			throw new InvalidDailyReadinessDateRangeException(
					"Date range must not exceed " + DailyAthleteStateSupport.MAX_HISTORY_DAYS + " days");
		}
	}

	private static ReadinessAlgorithmVersion parseAlgorithm(String algorithmVersion) {
		if (algorithmVersion == null || algorithmVersion.isBlank()) {
			return null;
		}
		try {
			return ReadinessAlgorithmVersion.valueOf(algorithmVersion.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidReadinessAlgorithmVersionException(
					"Unsupported readiness algorithm version: " + algorithmVersion);
		}
	}

}
