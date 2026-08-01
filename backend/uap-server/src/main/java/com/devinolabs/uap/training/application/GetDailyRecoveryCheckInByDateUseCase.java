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
public class GetDailyRecoveryCheckInByDateUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;

	public GetDailyRecoveryCheckInByDateUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
	}

	@Transactional(readOnly = true)
	public DailyRecoveryCheckInResult execute(AccountId accountId, LocalDate checkInDate) {
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		return checkInRepository.findByAthleteIdAndCheckInDate(AthleteId.of(athlete.athleteId()), checkInDate)
				.map(DailyRecoveryCheckInResult::from)
				.orElseThrow(RecoveryCheckInNotFoundException::new);
	}

}
