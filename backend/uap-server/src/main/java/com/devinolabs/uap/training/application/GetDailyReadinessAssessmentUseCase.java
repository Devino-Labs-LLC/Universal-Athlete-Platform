package com.devinolabs.uap.training.application;

import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;

@Service
public class GetDailyReadinessAssessmentUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyReadinessAssessmentRepository assessmentRepository;
	private final DailyAthleteStateSnapshotRepository snapshotRepository;

	public GetDailyReadinessAssessmentUseCase(
			AthleteContextPort athleteContextPort,
			DailyReadinessAssessmentRepository assessmentRepository,
			DailyAthleteStateSnapshotRepository snapshotRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.assessmentRepository = Objects.requireNonNull(assessmentRepository);
		this.snapshotRepository = Objects.requireNonNull(snapshotRepository);
	}

	@Transactional(readOnly = true)
	public DailyReadinessAssessmentResult execute(AccountId accountId, UUID assessmentId) {
		AthleteRef athlete = DailyAthleteStateSupport.requireReadableAthlete(
				athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		DailyReadinessAssessment assessment = assessmentRepository
				.findByIdAndAthleteId(DailyReadinessAssessmentId.of(assessmentId), athleteId)
				.orElseThrow(() -> new DailyReadinessAssessmentNotFoundException(
						"Daily readiness assessment not found: " + assessmentId));
		DailyAthleteStateSnapshot snapshot = snapshotRepository
				.findByIdAndAthleteId(assessment.dailyAthleteStateSnapshotId(), athleteId)
				.orElseThrow(() -> new DailyAthleteStateSnapshotNotFoundException(
						"Source snapshot not found for readiness assessment"));
		return DailyReadinessAssessmentResult.from(assessment, snapshot, false);
	}

}
