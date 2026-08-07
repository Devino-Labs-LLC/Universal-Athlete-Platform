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
import com.devinolabs.uap.training.domain.InvalidTrainingRecommendationAlgorithmVersionException;
import com.devinolabs.uap.training.domain.InvalidTrainingRecommendationDateRangeException;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;

@Service
public class GetDailyTrainingRecommendationHistoryUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyTrainingRecommendationRepository recommendationRepository;

	public GetDailyTrainingRecommendationHistoryUseCase(
			AthleteContextPort athleteContextPort,
			DailyTrainingRecommendationRepository recommendationRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.recommendationRepository = Objects.requireNonNull(recommendationRepository);
	}

	@Transactional(readOnly = true)
	public DailyTrainingRecommendationHistoryPage execute(
			AccountId accountId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			String algorithmVersion,
			String overallAction,
			Integer page,
			Integer size) {
		requireRange(startDate, endDate);
		TrainingRecommendationAlgorithmVersion version = parseAlgorithm(algorithmVersion);
		TrainingRecommendationAction action = parseAction(overallAction);
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		int pageNumber = RecoveryCheckInSupport.normalizePage(page);
		int pageSize = RecoveryCheckInSupport.normalizeSize(size);
		List<DailyTrainingRecommendationSummary> content = recommendationRepository.findHistory(
				athleteId, startDate, endDate, currentSnapshotOnly, version, action, pageNumber, pageSize);
		long total = recommendationRepository.countHistory(
				athleteId, startDate, endDate, currentSnapshotOnly, version, action);
		return new DailyTrainingRecommendationHistoryPage(content, pageNumber, pageSize, total);
	}

	private static void requireRange(LocalDate startDate, LocalDate endDate) {
		Objects.requireNonNull(startDate, "startDate must not be null");
		Objects.requireNonNull(endDate, "endDate must not be null");
		if (endDate.isBefore(startDate)) {
			throw new InvalidTrainingRecommendationDateRangeException("endDate must not be before startDate");
		}
		long span = ChronoUnit.DAYS.between(startDate, endDate) + 1;
		if (span > DailyAthleteStateSupport.MAX_HISTORY_DAYS) {
			throw new InvalidTrainingRecommendationDateRangeException(
					"Date range must not exceed " + DailyAthleteStateSupport.MAX_HISTORY_DAYS + " days");
		}
	}

	private static TrainingRecommendationAlgorithmVersion parseAlgorithm(String algorithmVersion) {
		if (algorithmVersion == null || algorithmVersion.isBlank()) {
			return null;
		}
		try {
			return TrainingRecommendationAlgorithmVersion.valueOf(algorithmVersion.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new InvalidTrainingRecommendationAlgorithmVersionException(
					"Unsupported training recommendation algorithm version: " + algorithmVersion);
		}
	}

	private static TrainingRecommendationAction parseAction(String overallAction) {
		if (overallAction == null || overallAction.isBlank()) {
			return null;
		}
		try {
			return TrainingRecommendationAction.valueOf(overallAction.trim().toUpperCase());
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unsupported overallAction filter: " + overallAction, ex);
		}
	}

}
