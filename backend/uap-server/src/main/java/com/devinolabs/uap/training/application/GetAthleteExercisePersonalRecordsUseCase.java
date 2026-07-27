package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.ExercisePerformanceKey;

@Service
public class GetAthleteExercisePersonalRecordsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final ExercisePerformanceHistoryRepository exercisePerformanceHistoryRepository;
	private final AthleteExercisePersonalRecordRepository personalRecordRepository;

	public GetAthleteExercisePersonalRecordsUseCase(
			AthleteContextPort athleteContextPort,
			ExercisePerformanceHistoryRepository exercisePerformanceHistoryRepository,
			AthleteExercisePersonalRecordRepository personalRecordRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.exercisePerformanceHistoryRepository = Objects.requireNonNull(exercisePerformanceHistoryRepository);
		this.personalRecordRepository = Objects.requireNonNull(personalRecordRepository);
	}

	@Transactional(readOnly = true)
	public List<PersonalRecordResult> execute(
			AccountId accountId,
			ExercisePerformanceKey exercisePerformanceKey) {
		Objects.requireNonNull(exercisePerformanceKey, "exercisePerformanceKey must not be null");
		AthleteRef athlete = TrainingPerformanceSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		if (!exercisePerformanceHistoryRepository.existsByAthleteIdAndExercisePerformanceKey(
				athleteId, exercisePerformanceKey)) {
			throw new ExercisePerformanceKeyNotFoundException();
		}
		return TrainingPerformanceSupport.toResults(
				personalRecordRepository.findAllByAthleteIdAndExercisePerformanceKey(
						athleteId, exercisePerformanceKey));
	}

}
