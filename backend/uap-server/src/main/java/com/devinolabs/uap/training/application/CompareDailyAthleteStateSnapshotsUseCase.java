package com.devinolabs.uap.training.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotComparisonService;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;

@Service
public class CompareDailyAthleteStateSnapshotsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;

	public CompareDailyAthleteStateSnapshotsUseCase(
			AthleteContextPort athleteContextPort,
			DailyAthleteStateSnapshotRepository snapshotRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
	}

	@Transactional(readOnly = true)
	public DailyAthleteStateSnapshotComparisonResult execute(
			AccountId accountId,
			UUID olderSnapshotId,
			UUID newerSnapshotId) {
		if (olderSnapshotId == null || newerSnapshotId == null) {
			throw new DailyAthleteStateSnapshotCompareInvalidException(
					"olderSnapshotId and newerSnapshotId are required");
		}
		if (olderSnapshotId.equals(newerSnapshotId)) {
			throw new DailyAthleteStateSnapshotCompareInvalidException(
					"olderSnapshotId and newerSnapshotId must be different");
		}
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyAthleteStateSnapshot older = snapshotRepository
				.findByIdAndAthleteId(DailyAthleteStateSnapshotId.of(olderSnapshotId), athleteId)
				.orElseThrow(() -> new DailyAthleteStateSnapshotNotFoundException(
						"Older snapshot not found: " + olderSnapshotId));
		DailyAthleteStateSnapshot newer = snapshotRepository
				.findByIdAndAthleteId(DailyAthleteStateSnapshotId.of(newerSnapshotId), athleteId)
				.orElseThrow(() -> new DailyAthleteStateSnapshotNotFoundException(
						"Newer snapshot not found: " + newerSnapshotId));
		if (newer.snapshotVersion() < older.snapshotVersion()
				&& newer.stateDate().equals(older.stateDate())) {
			throw new DailyAthleteStateSnapshotCompareInvalidException(
					"newerSnapshotId must not be an older version of the same date");
		}
		var comparison = DailyAthleteStateSnapshotComparisonService.compare(older, newer);
		return DailyAthleteStateSnapshotComparisonResult.from(comparison);
	}

}
