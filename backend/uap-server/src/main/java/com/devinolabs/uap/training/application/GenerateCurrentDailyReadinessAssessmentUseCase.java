package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;

@Service
public class GenerateCurrentDailyReadinessAssessmentUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;
	private final GenerateDailyReadinessAssessmentUseCase generateDailyReadinessAssessmentUseCase;

	public GenerateCurrentDailyReadinessAssessmentUseCase(
			AthleteContextPort athleteContextPort,
			DailyAthleteStateSnapshotRepository snapshotRepository,
			GenerateDailyReadinessAssessmentUseCase generateDailyReadinessAssessmentUseCase) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
		this.generateDailyReadinessAssessmentUseCase = Objects.requireNonNull(generateDailyReadinessAssessmentUseCase);
	}

	@Transactional
	public DailyReadinessAssessmentResult execute(AccountId accountId, LocalDate stateDate) {
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyAthleteStateSnapshot snapshot = snapshotRepository
				.findCurrentByAthleteIdAndStateDate(athleteId, stateDate)
				.orElseThrow(() -> new DailyReadinessStateSnapshotRequiredException(
						"A current daily athlete state snapshot is required for date " + stateDate));
		return generateDailyReadinessAssessmentUseCase.execute(accountId, snapshot.id().value());
	}

}
