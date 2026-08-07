package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DailyReadinessAssessmentRepository;
import com.devinolabs.uap.training.application.DailyReadinessAssessmentSummary;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyAthleteStateSnapshotId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessment;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.ReadinessAlgorithmVersion;

@Repository
class JpaDailyReadinessAssessmentRepository implements DailyReadinessAssessmentRepository {

	private final DailyReadinessAssessmentJpaRepository jpaRepository;
	private final DailyAthleteStateSnapshotJpaRepository snapshotJpaRepository;

	JpaDailyReadinessAssessmentRepository(
			DailyReadinessAssessmentJpaRepository jpaRepository,
			DailyAthleteStateSnapshotJpaRepository snapshotJpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
		this.snapshotJpaRepository = Objects.requireNonNull(snapshotJpaRepository);
	}

	@Override
	public DailyReadinessAssessment saveNew(DailyReadinessAssessment assessment) {
		try {
			DailyReadinessAssessmentJpaEntity entity =
					DailyReadinessAssessmentPersistenceMapper.toNewEntity(assessment);
			return toDomainWithChildren(jpaRepository.save(entity));
		}
		catch (DataIntegrityViolationException ex) {
			throw ex;
		}
	}

	@Override
	public Optional<DailyReadinessAssessment> findByIdAndAthleteId(
			DailyReadinessAssessmentId id,
			AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(this::toDomainWithChildren);
	}

	@Override
	public Optional<DailyReadinessAssessment> findBySnapshotIdAndAlgorithmVersion(
			DailyAthleteStateSnapshotId snapshotId,
			ReadinessAlgorithmVersion algorithmVersion,
			AthleteId athleteId) {
		return jpaRepository.findByDailyAthleteStateSnapshotIdAndAlgorithmVersionAndAthleteId(
						snapshotId.value(), algorithmVersion, athleteId.value())
				.map(this::toDomainWithChildren);
	}

	@Override
	public List<DailyReadinessAssessmentSummary> findHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			ReadinessAlgorithmVersion algorithmVersion,
			int page,
			int size) {
		List<DailyReadinessAssessmentJpaEntity> rows = jpaRepository.findHistory(
				athleteId.value(),
				startDate,
				endDate,
				currentSnapshotOnly,
				algorithmVersion,
				PageRequest.of(page, size));
		Set<UUID> currentSnapshotIds = currentSnapshotIds(rows);
		return rows.stream()
				.map(row -> DailyReadinessAssessmentPersistenceMapper.toSummary(
						row, currentSnapshotIds.contains(row.getDailyAthleteStateSnapshotId())))
				.toList();
	}

	@Override
	public long countHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			ReadinessAlgorithmVersion algorithmVersion) {
		return jpaRepository.countHistory(
				athleteId.value(), startDate, endDate, currentSnapshotOnly, algorithmVersion);
	}

	private Set<UUID> currentSnapshotIds(List<DailyReadinessAssessmentJpaEntity> rows) {
		Set<UUID> current = new HashSet<>();
		for (DailyReadinessAssessmentJpaEntity row : rows) {
			snapshotJpaRepository.findById(row.getDailyAthleteStateSnapshotId())
					.filter(DailyAthleteStateSnapshotJpaEntity::isCurrentSnapshot)
					.ifPresent(snapshot -> current.add(snapshot.getId()));
		}
		return current;
	}

	private DailyReadinessAssessment toDomainWithChildren(DailyReadinessAssessmentJpaEntity entity) {
		entity.getContributions().size();
		entity.getLimitingDimensions().size();
		entity.getStrongestDimensions().size();
		return DailyReadinessAssessmentPersistenceMapper.toDomain(entity);
	}

}
