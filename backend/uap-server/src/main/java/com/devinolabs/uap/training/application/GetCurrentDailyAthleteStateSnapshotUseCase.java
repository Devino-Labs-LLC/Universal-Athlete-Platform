package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;

@Service
public class GetCurrentDailyAthleteStateSnapshotUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;

	public GetCurrentDailyAthleteStateSnapshotUseCase(
			AthleteContextPort athleteContextPort,
			DailyAthleteStateSnapshotRepository snapshotRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
	}

	@Transactional(readOnly = true)
	public DailyAthleteStateSnapshotResult execute(AccountId accountId, LocalDate stateDate) {
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		return snapshotRepository.findCurrentByAthleteIdAndStateDate(athleteId, stateDate)
				.map(snapshot -> DailyAthleteStateSnapshotResult.from(snapshot, false))
				.orElseThrow(() -> new DailyAthleteStateSnapshotNotFoundException(
						"No current daily athlete state snapshot for date " + stateDate));
	}

}
