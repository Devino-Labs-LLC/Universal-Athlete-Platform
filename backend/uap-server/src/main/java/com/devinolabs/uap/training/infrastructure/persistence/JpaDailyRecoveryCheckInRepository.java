package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DailyRecoveryCheckInRepository;
import com.devinolabs.uap.training.application.RecoveryCheckInAlreadyExistsException;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.BodyArea;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckIn;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.RecoveryCheckInCompleteness;

@Repository
class JpaDailyRecoveryCheckInRepository implements DailyRecoveryCheckInRepository {

	private final DailyRecoveryCheckInJpaRepository jpaRepository;

	JpaDailyRecoveryCheckInRepository(DailyRecoveryCheckInJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public DailyRecoveryCheckIn save(DailyRecoveryCheckIn checkIn) {
		try {
			boolean isNew = !jpaRepository.existsById(checkIn.id().value());
			DailyRecoveryCheckInJpaEntity entity = jpaRepository.findById(checkIn.id().value())
					.map(existing -> {
						DailyRecoveryCheckInPersistenceMapper.applyMutableFields(existing, checkIn);
						return existing;
					})
					.orElseGet(() -> DailyRecoveryCheckInPersistenceMapper.toEntity(checkIn, isNew));
			return DailyRecoveryCheckInPersistenceMapper.toDomain(jpaRepository.save(entity));
		}
		catch (DataIntegrityViolationException ex) {
			throw mapConstraint(ex);
		}
	}

	@Override
	public Optional<DailyRecoveryCheckIn> findByIdAndAthleteId(DailyRecoveryCheckInId id, AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(DailyRecoveryCheckInPersistenceMapper::toDomain);
	}

	@Override
	public Optional<DailyRecoveryCheckIn> findByAthleteIdAndCheckInDate(AthleteId athleteId, LocalDate checkInDate) {
		return jpaRepository.findByAthleteIdAndCheckInDate(athleteId.value(), checkInDate)
				.map(DailyRecoveryCheckInPersistenceMapper::toDomain);
	}

	@Override
	public Optional<DailyRecoveryCheckIn> findByIdAndAthleteIdForUpdate(
			DailyRecoveryCheckInId id,
			AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteIdForUpdate(id.value(), athleteId.value())
				.map(DailyRecoveryCheckInPersistenceMapper::toDomain);
	}

	@Override
	public boolean existsByAthleteIdAndCheckInDate(AthleteId athleteId, LocalDate checkInDate) {
		return jpaRepository.existsByAthleteIdAndCheckInDate(athleteId.value(), checkInDate);
	}

	@Override
	public List<DailyRecoveryCheckIn> findByAthleteAndDateRange(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			RecoveryCheckInCompleteness completeness,
			Integer minimumFatigue,
			Integer minimumSoreness,
			BodyArea bodyArea,
			int page,
			int size) {
		return jpaRepository.findFiltered(
						athleteId.value(),
						startDate,
						endDate,
						completeness,
						minimumFatigue,
						minimumSoreness,
						bodyArea,
						PageRequest.of(page, size))
				.stream()
				.map(DailyRecoveryCheckInPersistenceMapper::toDomain)
				.toList();
	}

	@Override
	public long countByAthleteAndDateRange(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			RecoveryCheckInCompleteness completeness,
			Integer minimumFatigue,
			Integer minimumSoreness,
			BodyArea bodyArea) {
		return jpaRepository.countFiltered(
				athleteId.value(),
				startDate,
				endDate,
				completeness,
				minimumFatigue,
				minimumSoreness,
				bodyArea);
	}

	@Override
	public List<DailyRecoveryCheckIn> findAllByAthleteAndDateRange(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate) {
		return jpaRepository
				.findAllByAthleteIdAndCheckInDateBetweenOrderByCheckInDateDescIdAsc(
						athleteId.value(), startDate, endDate)
				.stream()
				.map(DailyRecoveryCheckInPersistenceMapper::toDomain)
				.toList();
	}

	private static RuntimeException mapConstraint(DataIntegrityViolationException ex) {
		String message = ex.getMostSpecificCause() == null ? "" : ex.getMostSpecificCause().getMessage();
		if (message != null && message.contains("uq_daily_recovery_check_ins_athlete_date")) {
			return new RecoveryCheckInAlreadyExistsException();
		}
		return ex;
	}

}
