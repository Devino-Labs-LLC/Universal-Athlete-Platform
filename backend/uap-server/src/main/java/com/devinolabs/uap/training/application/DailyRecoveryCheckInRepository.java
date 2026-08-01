package com.devinolabs.uap.training.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;

public interface DailyRecoveryCheckInRepository {

	DailyRecoveryCheckIn save(DailyRecoveryCheckIn checkIn);

	Optional<DailyRecoveryCheckIn> findByIdAndAthleteId(DailyRecoveryCheckInId id, AthleteId athleteId);

	Optional<DailyRecoveryCheckIn> findByAthleteIdAndCheckInDate(AthleteId athleteId, LocalDate checkInDate);

	Optional<DailyRecoveryCheckIn> findByIdAndAthleteIdForUpdate(DailyRecoveryCheckInId id, AthleteId athleteId);

	boolean existsByAthleteIdAndCheckInDate(AthleteId athleteId, LocalDate checkInDate);

	List<DailyRecoveryCheckIn> findByAthleteAndDateRange(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			RecoveryCheckInCompleteness completeness,
			Integer minimumFatigue,
			Integer minimumSoreness,
			BodyArea bodyArea,
			int page,
			int size);

	long countByAthleteAndDateRange(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			RecoveryCheckInCompleteness completeness,
			Integer minimumFatigue,
			Integer minimumSoreness,
			BodyArea bodyArea);

	List<DailyRecoveryCheckIn> findAllByAthleteAndDateRange(AthleteId athleteId, LocalDate startDate, LocalDate endDate);

}
