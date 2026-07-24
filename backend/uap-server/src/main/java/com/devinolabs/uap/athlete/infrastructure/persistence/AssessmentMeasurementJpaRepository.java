package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface AssessmentMeasurementJpaRepository extends JpaRepository<AssessmentMeasurementJpaEntity, UUID> {

	Optional<AssessmentMeasurementJpaEntity> findByIdAndAssessmentIdAndAthleteId(
			UUID id,
			UUID assessmentId,
			UUID athleteId);

	List<AssessmentMeasurementJpaEntity> findAllByAssessmentIdAndAthleteIdOrderByDisplayOrderAscCreatedAtAscIdAsc(
			UUID assessmentId,
			UUID athleteId);

	boolean existsByAssessmentIdAndSourceMeasurementId(UUID assessmentId, UUID sourceMeasurementId);

	long countByAssessmentIdAndAthleteId(UUID assessmentId, UUID athleteId);

	void deleteByIdAndAssessmentIdAndAthleteId(UUID id, UUID assessmentId, UUID athleteId);

	@Query("""
			select case when count(am) > 0 then true else false end
			from AssessmentMeasurementJpaEntity am, AssessmentJpaEntity a
			where am.sourceMeasurementId = :sourceMeasurementId
			and am.assessmentId = a.id
			and a.status <> com.devinolabs.uap.athlete.domain.AssessmentStatus.COMPLETED
			""")
	boolean existsActiveAttachmentBySourceMeasurementId(@Param("sourceMeasurementId") UUID sourceMeasurementId);

	@Query("""
			select coalesce(max(am.displayOrder), -1)
			from AssessmentMeasurementJpaEntity am
			where am.assessmentId = :assessmentId
			and am.athleteId = :athleteId
			""")
	int findMaxDisplayOrder(@Param("assessmentId") UUID assessmentId, @Param("athleteId") UUID athleteId);

	List<AssessmentMeasurementJpaEntity> findAllByIdInAndAssessmentIdAndAthleteId(
			Collection<UUID> ids,
			UUID assessmentId,
			UUID athleteId);

}
