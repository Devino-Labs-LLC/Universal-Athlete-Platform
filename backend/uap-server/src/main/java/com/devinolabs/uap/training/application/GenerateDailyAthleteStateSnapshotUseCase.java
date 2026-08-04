package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.DailyAthleteStateGenerationReason;

/**
 * Explicit generation entrypoint. Phase 7Q does not auto-refresh after source mutations.
 */
@Service
public class GenerateDailyAthleteStateSnapshotUseCase {

	private final DailyAthleteStateGenerationService generationService;

	public GenerateDailyAthleteStateSnapshotUseCase(DailyAthleteStateGenerationService generationService) {
		this.generationService = Objects.requireNonNull(generationService);
	}

	@Transactional
	public DailyAthleteStateSnapshotResult execute(AccountId accountId, LocalDate stateDate, int baselineWindowDays) {
		return generationService.generate(
				accountId, stateDate, baselineWindowDays, DailyAthleteStateGenerationReason.MANUAL);
	}

}
