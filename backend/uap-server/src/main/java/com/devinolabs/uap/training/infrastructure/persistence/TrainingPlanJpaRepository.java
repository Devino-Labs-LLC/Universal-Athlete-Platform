package com.devinolabs.uap.training.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.TrainingPlanStatus;
import com.devinolabs.uap.training.domain.TrainingPlanType;

interface TrainingPlanJpaRepository extends JpaRepository<TrainingPlanJpaEntity, UUID> {

	Optional<TrainingPlanJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	List<TrainingPlanJpaEntity> findAllByAthleteIdOrderByStartDateDescCreatedAtDescIdAsc(UUID athleteId);

	@Query("""
			select case when count(p) > 0 then true else false end
			from TrainingPlanJpaEntity p
			where p.athleteId = :athleteId
			and p.normalizedName = :normalizedName
			and p.status <> com.devinolabs.uap.training.domain.TrainingPlanStatus.ARCHIVED
			and p.startDate <= :endDate
			and p.endDate >= :startDate
			and (:excludingId is null or p.id <> :excludingId)
			""")
	boolean existsOverlappingDuplicate(
			@Param("athleteId") UUID athleteId,
			@Param("normalizedName") String normalizedName,
			@Param("startDate") LocalDate startDate,
			@Param("endDate") LocalDate endDate,
			@Param("excludingId") UUID excludingId);

	@Query("""
			select p from TrainingPlanJpaEntity p
			where p.athleteId = :athleteId
			and (:status is null or p.status = :status)
			and (:planType is null or p.planType = :planType)
			order by p.startDate desc, p.createdAt desc, p.id asc
			""")
	List<TrainingPlanJpaEntity> findFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("status") TrainingPlanStatus status,
			@Param("planType") TrainingPlanType planType);

}
