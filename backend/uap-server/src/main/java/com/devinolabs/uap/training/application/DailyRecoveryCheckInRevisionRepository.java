package com.devinolabs.uap.training.application;

import java.util.List;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInRevision;

public interface DailyRecoveryCheckInRevisionRepository {

	DailyRecoveryCheckInRevision save(DailyRecoveryCheckInRevision revision);

	List<DailyRecoveryCheckInRevision> findAllByCheckInIdAndAthleteIdOrderByRevisionNumber(
			DailyRecoveryCheckInId checkInId,
			AthleteId athleteId);

	int countByCheckInId(DailyRecoveryCheckInId checkInId);

}
