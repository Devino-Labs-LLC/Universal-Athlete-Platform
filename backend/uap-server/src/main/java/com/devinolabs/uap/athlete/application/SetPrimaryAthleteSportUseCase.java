package com.devinolabs.uap.athlete.application;

import java.time.Clock;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteSport;
import com.devinolabs.uap.athlete.domain.AthleteSportId;

@Service
public class SetPrimaryAthleteSportUseCase {

	private final AthleteRepository athleteRepository;
	private final AthleteSportRepository athleteSportRepository;
	private final Clock clock;

	public SetPrimaryAthleteSportUseCase(
			AthleteRepository athleteRepository,
			AthleteSportRepository athleteSportRepository,
			Clock clock) {
		this.athleteRepository = Objects.requireNonNull(athleteRepository);
		this.athleteSportRepository = Objects.requireNonNull(athleteSportRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public AthleteSportResult execute(AccountId accountId, AthleteSportId sportId) {
		Athlete athlete = AthleteSportSupport.requireMutableAthlete(athleteRepository, accountId);
		AthleteSport target = athleteSportRepository.findByIdAndAthleteId(sportId, athlete.id())
				.orElseThrow(AthleteSportNotFoundException::new);

		if (target.primarySport()) {
			return AthleteSportSupport.toResult(target);
		}

		Optional<AthleteSport> currentPrimary = athleteSportRepository.findPrimaryByAthleteId(athlete.id());
		if (currentPrimary.isPresent()) {
			AthleteSport previous = currentPrimary.get();
			previous.unmarkPrimary(clock);
			athleteSportRepository.save(previous);
		}

		target.markPrimary(clock);
		return AthleteSportSupport.toResult(athleteSportRepository.save(target));
	}

}
