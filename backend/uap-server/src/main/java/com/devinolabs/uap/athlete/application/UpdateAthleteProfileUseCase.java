package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Weight;

@Service
public class UpdateAthleteProfileUseCase {

	private final AthleteRepository athleteRepository;
	private final Clock clock;

	public UpdateAthleteProfileUseCase(AthleteRepository athleteRepository, Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AthleteProfileResult execute(
			AccountId accountId,
			String firstName,
			String lastName,
			Height height,
			Weight weight,
			DominantHand dominantHand,
			DominantFoot dominantFoot) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		Athlete athlete = athleteRepository.findByAccountId(accountId)
				.orElseThrow(AthleteProfileNotFoundException::new);

		athlete.rename(firstName, lastName, clock);
		athlete.updateHeight(height, clock);
		athlete.updateWeight(weight, clock);
		athlete.updateDominantHand(dominantHand, clock);
		athlete.updateDominantFoot(dominantFoot, clock);

		return CreateAthleteProfileUseCase.toResult(athleteRepository.save(athlete));
	}

}
