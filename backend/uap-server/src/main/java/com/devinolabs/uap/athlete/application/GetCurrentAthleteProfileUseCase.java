package com.devinolabs.uap.athlete.application;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;

@Service
public class GetCurrentAthleteProfileUseCase {

	private final AthleteRepository athleteRepository;

	public GetCurrentAthleteProfileUseCase(AthleteRepository athleteRepository) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
	}

	@Transactional(readOnly = true)
	public AthleteProfileResult execute(AccountId accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Athlete athlete = athleteRepository.findByAccountId(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);
		return CreateAthleteProfileUseCase.toResult(athlete);
	}

}
