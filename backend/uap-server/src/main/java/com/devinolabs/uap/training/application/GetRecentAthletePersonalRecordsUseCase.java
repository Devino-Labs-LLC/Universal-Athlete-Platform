package com.devinolabs.uap.training.application;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;

/**
 * Current personal records the athlete actually set inside a recency window.
 *
 * <p>This reads the current projection rather than the history log, so a record that has since
 * been beaten never shows up even if it was beaten inside the window.
 */
@Service
public class GetRecentAthletePersonalRecordsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final AthleteExercisePersonalRecordRepository personalRecordRepository;
	private final Clock clock;

	public GetRecentAthletePersonalRecordsUseCase(
			AthleteContextPort athleteContextPort,
			AthleteExercisePersonalRecordRepository personalRecordRepository,
			Clock clock) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.personalRecordRepository = Objects.requireNonNull(personalRecordRepository);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public List<PersonalRecordResult> execute(AccountId accountId, Integer days, Integer limit) {
		AthleteRef athlete = TrainingPerformanceSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		int resolvedDays = TrainingPerformanceSupport.requireRecentDays(days);
		int resolvedLimit = TrainingPerformanceSupport.requireRecentLimit(limit);
		Instant achievedFrom = Instant.now(clock).minus(Duration.ofDays(resolvedDays));
		return TrainingPerformanceSupport.toResults(
				personalRecordRepository.findRecentByAthleteId(athleteId, achievedFrom, resolvedLimit));
	}

}
