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
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;

@Service
public class ListDailyRecoveryCheckInsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;

	public ListDailyRecoveryCheckInsUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
	}

	@Transactional(readOnly = true)
	public DailyRecoveryCheckInListResult execute(
			AccountId accountId,
			LocalDate startDate,
			LocalDate endDate,
			RecoveryCheckInCompleteness completeness,
			Integer minimumFatigue,
			Integer minimumSoreness,
			BodyArea bodyArea,
			Integer page,
			Integer size) {
		RecoveryCheckInSupport.requireListDateRange(startDate, endDate);
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		int resolvedPage = RecoveryCheckInSupport.normalizePage(page);
		int resolvedSize = RecoveryCheckInSupport.normalizeSize(size);
		long total = checkInRepository.countByAthleteAndDateRange(
				athleteId, startDate, endDate, completeness, minimumFatigue, minimumSoreness, bodyArea);
		List<DailyRecoveryCheckInResult> checkIns = checkInRepository.findByAthleteAndDateRange(
						athleteId,
						startDate,
						endDate,
						completeness,
						minimumFatigue,
						minimumSoreness,
						bodyArea,
						resolvedPage,
						resolvedSize)
				.stream()
				.map(DailyRecoveryCheckInResult::from)
				.toList();
		int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / resolvedSize);
		return new DailyRecoveryCheckInListResult(checkIns, resolvedPage, resolvedSize, total, totalPages);
	}

}
