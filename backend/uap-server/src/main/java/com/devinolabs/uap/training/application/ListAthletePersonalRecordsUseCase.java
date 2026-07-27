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
import com.devinolabs.uap.training.domain.PersonalRecordType;

@Service
public class ListAthletePersonalRecordsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final AthleteExercisePersonalRecordRepository personalRecordRepository;

	public ListAthletePersonalRecordsUseCase(
			AthleteContextPort athleteContextPort,
			AthleteExercisePersonalRecordRepository personalRecordRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.personalRecordRepository = Objects.requireNonNull(personalRecordRepository);
	}

	@Transactional(readOnly = true)
	public List<PersonalRecordResult> execute(
			AccountId accountId,
			ExercisePerformanceKey exercisePerformanceKey,
			PersonalRecordType recordType) {
		AthleteRef athlete = TrainingPerformanceSupport.requireAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		return TrainingPerformanceSupport.toResults(
				personalRecordRepository.findAllByAthleteId(athleteId, exercisePerformanceKey, recordType));
	}

}
