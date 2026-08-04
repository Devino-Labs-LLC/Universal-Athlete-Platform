package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.DailyAthleteStateGenerationReason;

@Service
public class RegenerateDailyAthleteStateSnapshotUseCase {

	private final DailyAthleteStateGenerationService generationService;

	public RegenerateDailyAthleteStateSnapshotUseCase(DailyAthleteStateGenerationService generationService) {
		this.generationService = Objects.requireNonNull(generationService);
	}

	@Transactional
	public DailyAthleteStateSnapshotResult execute(AccountId accountId, LocalDate stateDate, int baselineWindowDays) {
		return generationService.generate(
				accountId, stateDate, baselineWindowDays, DailyAthleteStateGenerationReason.REGENERATED);
	}

}
