package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;

public interface DailyAthleteStateSnapshotRepository {

	DailyAthleteStateSnapshot saveNew(DailyAthleteStateSnapshot snapshot);

	void markNotCurrent(DailyAthleteStateSnapshotId id, AthleteId athleteId);

	Optional<DailyAthleteStateSnapshot> findCurrentByAthleteIdAndStateDate(AthleteId athleteId, LocalDate stateDate);

	Optional<DailyAthleteStateSnapshot> findCurrentByAthleteIdAndStateDateForUpdate(
			AthleteId athleteId,
			LocalDate stateDate);

	Optional<DailyAthleteStateSnapshot> findByIdAndAthleteId(DailyAthleteStateSnapshotId id, AthleteId athleteId);

	int nextSnapshotVersion(AthleteId athleteId, LocalDate stateDate);

	List<DailyAthleteStateSnapshotSummary> listVersions(AthleteId athleteId, LocalDate stateDate);

	List<DailyAthleteStateSnapshotSummary> findHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentOnly,
			int page,
			int size);

	long countHistory(AthleteId athleteId, LocalDate startDate, LocalDate endDate, boolean currentOnly);

}
