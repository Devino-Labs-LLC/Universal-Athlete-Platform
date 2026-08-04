package com.devinolabs.uap.training.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;

@Service
public class GetDailyAthleteStateSnapshotUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;

	public GetDailyAthleteStateSnapshotUseCase(
			AthleteContextPort athleteContextPort,
			DailyAthleteStateSnapshotRepository snapshotRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
	}

	@Transactional(readOnly = true)
	public DailyAthleteStateSnapshotResult execute(AccountId accountId, UUID snapshotId) {
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		return snapshotRepository.findByIdAndAthleteId(DailyAthleteStateSnapshotId.of(snapshotId), athleteId)
				.map(snapshot -> DailyAthleteStateSnapshotResult.from(snapshot, false))
				.orElseThrow(() -> new DailyAthleteStateSnapshotNotFoundException(
						"Daily athlete state snapshot not found: " + snapshotId));
	}

}
