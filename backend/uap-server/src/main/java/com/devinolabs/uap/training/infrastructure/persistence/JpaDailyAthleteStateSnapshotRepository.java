package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotNotFoundException;
import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotRepository;
import com.devinolabs.uap.training.application.DailyAthleteStateSnapshotSummary;
import com.devinolabs.uap.training.application.DailyAthleteStateVersionConflictException;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshot;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;

@Repository
class JpaDailyAthleteStateSnapshotRepository implements DailyAthleteStateSnapshotRepository {

	private final DailyAthleteStateSnapshotJpaRepository jpaRepository;

	JpaDailyAthleteStateSnapshotRepository(DailyAthleteStateSnapshotJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public DailyAthleteStateSnapshot saveNew(DailyAthleteStateSnapshot snapshot) {
		try {
			DailyAthleteStateSnapshotJpaEntity entity =
					DailyAthleteStateSnapshotPersistenceMapper.toNewEntity(snapshot);
			return DailyAthleteStateSnapshotPersistenceMapper.toDomain(jpaRepository.save(entity));
		}
		catch (DataIntegrityViolationException ex) {
			throw new DailyAthleteStateVersionConflictException(
					"Concurrent daily athlete state snapshot version conflict", ex);
		}
	}

	@Override
	public void markNotCurrent(DailyAthleteStateSnapshotId id, AthleteId athleteId) {
		DailyAthleteStateSnapshotJpaEntity entity = jpaRepository
				.findByIdAndAthleteId(id.value(), athleteId.value())
				.orElseThrow(() -> new DailyAthleteStateSnapshotNotFoundException(
						"Daily athlete state snapshot not found: " + id));
		entity.setCurrentSnapshot(false);
		jpaRepository.save(entity);
	}

	@Override
	public Optional<DailyAthleteStateSnapshot> findCurrentByAthleteIdAndStateDate(
			AthleteId athleteId,
			LocalDate stateDate) {
		return jpaRepository.findByAthleteIdAndStateDateAndCurrentSnapshotTrue(athleteId.value(), stateDate)
				.map(this::toDomainWithChildren);
	}

	@Override
	public Optional<DailyAthleteStateSnapshotSummary> findCurrentSummaryByAthleteIdAndStateDate(
			AthleteId athleteId,
			LocalDate stateDate) {
		return jpaRepository.findByAthleteIdAndStateDateAndCurrentSnapshotTrue(athleteId.value(), stateDate)
				.map(DailyAthleteStateSnapshotPersistenceMapper::toSummary);
	}

	@Override
	public Optional<DailyAthleteStateSnapshot> findCurrentByAthleteIdAndStateDateForUpdate(
			AthleteId athleteId,
			LocalDate stateDate) {
		return jpaRepository.findCurrentForUpdate(athleteId.value(), stateDate)
				.map(this::toDomainWithChildren);
	}

	@Override
	public Optional<DailyAthleteStateSnapshot> findByIdAndAthleteId(
			DailyAthleteStateSnapshotId id,
			AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(this::toDomainWithChildren);
	}

	private DailyAthleteStateSnapshot toDomainWithChildren(DailyAthleteStateSnapshotJpaEntity entity) {
		JpaAssociationInitializer.initialize(entity.getRecoveryMetrics());
		JpaAssociationInitializer.initialize(entity.getDiscomfort());
		JpaAssociationInitializer.initialize(entity.getCategories());
		JpaAssociationInitializer.initialize(entity.getMovements());
		JpaAssociationInitializer.initialize(entity.getScheduledOccurrences());
		return DailyAthleteStateSnapshotPersistenceMapper.toDomain(entity);
	}

	@Override
	public int nextSnapshotVersion(AthleteId athleteId, LocalDate stateDate) {
		return jpaRepository.findMaxSnapshotVersion(athleteId.value(), stateDate) + 1;
	}

	@Override
	public List<DailyAthleteStateSnapshotSummary> listVersions(AthleteId athleteId, LocalDate stateDate) {
		return jpaRepository.findByAthleteIdAndStateDateOrderBySnapshotVersionDesc(athleteId.value(), stateDate)
				.stream()
				.map(DailyAthleteStateSnapshotPersistenceMapper::toSummary)
				.toList();
	}

	@Override
	public List<DailyAthleteStateSnapshotSummary> findHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentOnly,
			int page,
			int size) {
		return jpaRepository.findHistory(
						athleteId.value(),
						startDate,
						endDate,
						currentOnly,
						PageRequest.of(page, size))
				.stream()
				.map(DailyAthleteStateSnapshotPersistenceMapper::toSummary)
				.toList();
	}

	@Override
	public long countHistory(AthleteId athleteId, LocalDate startDate, LocalDate endDate, boolean currentOnly) {
		return jpaRepository.countHistory(athleteId.value(), startDate, endDate, currentOnly);
	}

}
