package com.devinolabs.uap.training.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteNotFoundException;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.DistanceUnit;
import com.devinolabs.uap.training.domain.ReadinessCalculator;
import com.devinolabs.uap.training.domain.RecoveryCheckInDateValidator;
import com.devinolabs.uap.training.domain.TrainingClientContractVersion;
import com.devinolabs.uap.training.domain.TrainingRecommendationCalculator;
import com.devinolabs.uap.training.domain.WeightUnit;

@Service
public class GetTrainingClientBootstrapUseCase {

	private final AthleteContextPort athleteContextPort;

	public GetTrainingClientBootstrapUseCase(AthleteContextPort athleteContextPort) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
	}

	@Transactional(readOnly = true)
	public TrainingClientBootstrapResult execute(AccountId accountId) {
		try {
			TrainingClientFacadeSupport.requireReadableAthlete(athleteContextPort, accountId.value());
			return new TrainingClientBootstrapResult(
					TrainingClientContractVersion.V1,
					new TrainingClientBootstrapResult.Features(true, true, true, true, true, true),
					new TrainingClientBootstrapResult.Limits(
							RecoveryCheckInSupport.MAX_LIST_RANGE_DAYS,
							List.of(7, 14, 28),
							ReadinessCalculator.ALGORITHM_VERSION,
							TrainingRecommendationCalculator.ALGORITHM_VERSION,
							TrainingEnvironmentSupport.MAX_PAGE_SIZE,
							DailyAthleteStateSupport.MAX_HISTORY_DAYS,
							RecoveryCheckInDateValidator.MAX_PAST_DAYS),
					new TrainingClientBootstrapResult.Units(
							WeightUnit.KILOGRAM,
							DistanceUnit.METER,
							"SECONDS",
							"SESSION_RPE_LOAD"),
					new TrainingClientBootstrapResult.RatingScales(
							1,
							5,
							new BigDecimal("0.0"),
							new BigDecimal("10.0")));
		}
		catch (AthleteNotFoundException ex) {
			throw ex;
		}
		catch (RuntimeException ex) {
			throw new TrainingClientBootstrapFailedException("Failed to load training client bootstrap", ex);
		}
	}

}
