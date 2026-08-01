package com.devinolabs.uap.training.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;

@Service
public class GetDailyRecoveryCheckInUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;

	public GetDailyRecoveryCheckInUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
	}

	@Transactional(readOnly = true)
	public DailyRecoveryCheckInResult execute(AccountId accountId, DailyRecoveryCheckInId checkInId) {
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		return checkInRepository.findByIdAndAthleteId(checkInId, AthleteId.of(athlete.athleteId()))
				.map(DailyRecoveryCheckInResult::from)
				.orElseThrow(RecoveryCheckInNotFoundException::new);
	}

}
