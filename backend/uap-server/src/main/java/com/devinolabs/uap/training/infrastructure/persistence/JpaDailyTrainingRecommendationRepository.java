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

import com.devinolabs.uap.training.application.DailyTrainingRecommendationRepository;
import com.devinolabs.uap.training.application.DailyTrainingRecommendationSummary;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyReadinessAssessmentId;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendation;
import com.devinolabs.uap.training.domain.DailyTrainingRecommendationId;
import com.devinolabs.uap.training.domain.TrainingRecommendationAction;
import com.devinolabs.uap.training.domain.TrainingRecommendationAlgorithmVersion;

@Repository
class JpaDailyTrainingRecommendationRepository implements DailyTrainingRecommendationRepository {

	private final DailyTrainingRecommendationJpaRepository jpaRepository;
	private final DailyAthleteStateSnapshotJpaRepository snapshotJpaRepository;

	JpaDailyTrainingRecommendationRepository(
			DailyTrainingRecommendationJpaRepository jpaRepository,
			DailyAthleteStateSnapshotJpaRepository snapshotJpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
		this.snapshotJpaRepository = Objects.requireNonNull(snapshotJpaRepository);
	}

	@Override
	public DailyTrainingRecommendation saveNew(DailyTrainingRecommendation recommendation) {
		try {
			DailyTrainingRecommendationJpaEntity entity =
					DailyTrainingRecommendationPersistenceMapper.toNewEntity(recommendation);
			return toDomainWithChildren(jpaRepository.save(entity));
		}
		catch (DataIntegrityViolationException ex) {
			throw ex;
		}
	}

	@Override
	public Optional<DailyTrainingRecommendation> findByIdAndAthleteId(
			DailyTrainingRecommendationId id,
			AthleteId athleteId) {
		return jpaRepository.findByIdAndAthleteId(id.value(), athleteId.value())
				.map(this::toDomainWithChildren);
	}

	@Override
	public Optional<DailyTrainingRecommendation> findByAssessmentIdAndAlgorithmVersion(
			DailyReadinessAssessmentId assessmentId,
			TrainingRecommendationAlgorithmVersion algorithmVersion,
			AthleteId athleteId) {
		return jpaRepository.findByDailyReadinessAssessmentIdAndRecommendationAlgorithmVersionAndAthleteId(
						assessmentId.value(), algorithmVersion, athleteId.value())
				.map(this::toDomainWithChildren);
	}

	@Override
	public List<DailyTrainingRecommendationSummary> findHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			TrainingRecommendationAlgorithmVersion algorithmVersion,
			TrainingRecommendationAction overallAction,
			int page,
			int size) {
		List<DailyTrainingRecommendationJpaEntity> rows = jpaRepository.findHistory(
				athleteId.value(),
				startDate,
				endDate,
				currentSnapshotOnly,
				algorithmVersion,
				overallAction,
				PageRequest.of(page, size));
		Set<UUID> currentSnapshotIds = currentSnapshotIds(rows);
		return rows.stream()
				.map(row -> DailyTrainingRecommendationPersistenceMapper.toSummary(
						row, currentSnapshotIds.contains(row.getDailyAthleteStateSnapshotId())))
				.toList();
	}

	@Override
	public long countHistory(
			AthleteId athleteId,
			LocalDate startDate,
			LocalDate endDate,
			boolean currentSnapshotOnly,
			TrainingRecommendationAlgorithmVersion algorithmVersion,
			TrainingRecommendationAction overallAction) {
		return jpaRepository.countHistory(
				athleteId.value(),
				startDate,
				endDate,
				currentSnapshotOnly,
				algorithmVersion,
				overallAction);
	}

	private Set<UUID> currentSnapshotIds(List<DailyTrainingRecommendationJpaEntity> rows) {
		Set<UUID> current = new HashSet<>();
		for (DailyTrainingRecommendationJpaEntity row : rows) {
			snapshotJpaRepository.findById(row.getDailyAthleteStateSnapshotId())
					.filter(DailyAthleteStateSnapshotJpaEntity::isCurrentSnapshot)
					.ifPresent(snapshot -> current.add(snapshot.getId()));
		}
		return current;
	}

	private DailyTrainingRecommendation toDomainWithChildren(DailyTrainingRecommendationJpaEntity entity) {
		entity.getAdjustments().size();
		entity.getOccurrences().size();
		for (DailyTrainingRecommendationAdjustmentJpaEntity adjustment : entity.getAdjustments()) {
			adjustment.getReasons().size();
			adjustment.getDimensions().size();
		}
		return DailyTrainingRecommendationPersistenceMapper.toDomain(entity);
	}

}
