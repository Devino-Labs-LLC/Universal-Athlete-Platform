package com.devinolabs.uap.training.application;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.training.domain.AccountId;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;

@Service
public class ListDailyRecoveryCheckInRevisionsUseCase {

	private final AthleteContextPort athleteContextPort;
	private final DailyRecoveryCheckInRepository checkInRepository;
	private final DailyRecoveryCheckInRevisionRepository revisionRepository;

	public ListDailyRecoveryCheckInRevisionsUseCase(
			AthleteContextPort athleteContextPort,
			DailyRecoveryCheckInRepository checkInRepository,
			DailyRecoveryCheckInRevisionRepository revisionRepository) {
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.checkInRepository = Objects.requireNonNull(checkInRepository);
		this.revisionRepository = Objects.requireNonNull(revisionRepository);
	}

	@Transactional(readOnly = true)
	public List<DailyRecoveryCheckInRevisionResult> execute(
			AccountId accountId,
			DailyRecoveryCheckInId checkInId) {
		AthleteRef athlete = RecoveryCheckInSupport.requireReadableAthlete(athleteContextPort, accountId.value());
		AthleteId athleteId = AthleteId.of(athlete.athleteId());
		if (checkInRepository.findByIdAndAthleteId(checkInId, athleteId).isEmpty()) {
			throw new RecoveryCheckInNotFoundException();
		}
		return revisionRepository.findAllByCheckInIdAndAthleteIdOrderByRevisionNumber(checkInId, athleteId)
				.stream()
				.map(DailyRecoveryCheckInRevisionResult::from)
				.toList();
	}

}
