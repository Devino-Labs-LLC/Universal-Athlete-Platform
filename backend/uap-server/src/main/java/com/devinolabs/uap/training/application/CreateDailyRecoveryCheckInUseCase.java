package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyAreaDiscomfortObservation;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.FatigueRating;
import com.devinolabs.uap.training.domain.MoodRating;
import com.devinolabs.uap.training.domain.MuscleSorenessRating;
import com.devinolabs.uap.training.domain.RecoveryCheckInSource;
import com.devinolabs.uap.training.domain.SleepQualityRating;
import com.devinolabs.uap.training.domain.StressRating;
import com.devinolabs.uap.training.domain.TrainingMotivationRating;

@Service
public class CreateDailyRecoveryCheckInUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final java.time.Clock clock;

	public CreateDailyRecoveryCheckInUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository,
			java.time.Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public DailyRecoveryCheckInResult execute(
			AccountId accountId,
			LocalDate checkInDate,
			Integer sleepDurationMinutes,
			Integer sleepQuality,
			int fatigue,
			int muscleSoreness,
			int stress,
			int mood,
			int motivation,
			List<BodyAreaDiscomfortObservation.Input> discomfortInputs,
			String notes) {
		AthleteRef athlete = RecoveryCheckInSupport.requireMutableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		if (checkInRepository.existsByAthleteIdAndCheckInDate(athleteId, checkInDate)) {
			throw new RecoveryCheckInAlreadyExistsException();
		}
		List<BodyAreaDiscomfortObservation> discomfort = discomfortInputs == null
				? List.of()
				: BodyAreaDiscomfortObservation.validateAndOrder(discomfortInputs);
		DailyRecoveryCheckIn checkIn = DailyRecoveryCheckIn.create(
				athleteId,
				checkInDate,
				sleepDurationMinutes,
				sleepQuality == null ? null : SleepQualityRating.of(sleepQuality),
				FatigueRating.of(fatigue),
				MuscleSorenessRating.of(muscleSoreness),
				StressRating.of(stress),
				MoodRating.of(mood),
				TrainingMotivationRating.of(motivation),
				discomfort,
				notes,
				RecoveryCheckInSource.ATHLETE_REPORTED,
				clock);
		return DailyRecoveryCheckInResult.from(checkInRepository.save(checkIn));
	}

}
