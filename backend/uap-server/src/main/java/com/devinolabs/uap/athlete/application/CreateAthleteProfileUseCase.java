package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;
import com.devinolabs.uap.athlete.domain.DominantFoot;
import com.devinolabs.uap.athlete.domain.DominantHand;
import com.devinolabs.uap.athlete.domain.Height;
import com.devinolabs.uap.athlete.domain.Sex;
import com.devinolabs.uap.athlete.domain.Weight;

@Service
public class CreateAthleteProfileUseCase {

	private final AthleteRepository athleteRepository;
	private final Clock clock;

	public CreateAthleteProfileUseCase(AthleteRepository athleteRepository, Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AthleteProfileResult execute(
			AccountId accountId,
			String firstName,
			String lastName,
			LocalDate dateOfBirth,
			Sex sex,
			Height height,
			Weight weight,
			DominantHand dominantHand,
			DominantFoot dominantFoot) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		if (athleteRepository.existsByAccountId(accountId)) {
			throw new DuplicateAthleteProfileException();
		}

		Athlete athlete = Athlete.register(
				AthleteId.generate(),
				accountId,
				firstName,
				lastName,
				dateOfBirth,
				sex,
				height,
				weight,
				dominantHand,
				dominantFoot,
				clock);
		return toResult(athleteRepository.save(athlete));
	}

	static AthleteProfileResult toResult(Athlete athlete) {
		return new AthleteProfileResult(
				athlete.id(),
				athlete.firstName(),
				athlete.lastName(),
				athlete.dateOfBirth(),
				athlete.sex(),
				athlete.height(),
				athlete.weight(),
				athlete.dominantHand(),
				athlete.dominantFoot(),
				athlete.status(),
				athlete.createdAt(),
				athlete.updatedAt());
	}

}
