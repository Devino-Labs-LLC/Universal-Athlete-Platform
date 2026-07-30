package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.training.domain.EquipmentType;
import com.devinolabs.uap.training.domain.TrainingEnvironmentType;

interface TrainingEnvironmentJpaRepository extends JpaRepository<TrainingEnvironmentJpaEntity, UUID> {

	Optional<TrainingEnvironmentJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	@Query("""
			select count(e) from TrainingEnvironmentJpaEntity e
			where e.athleteId = :athleteId and e.active = true and e.normalizedName = :normalizedName
			and (:excludingId is null or e.id <> :excludingId)
			""")
	long countActiveByAthleteIdAndNormalizedName(
			@Param("athleteId") UUID athleteId,
			@Param("normalizedName") String normalizedName,
			@Param("excludingId") UUID excludingId);

	@Query("select count(e) > 0 from TrainingEnvironmentJpaEntity e where e.athleteId = :athleteId and e.active = true")
	boolean existsActiveByAthleteId(@Param("athleteId") UUID athleteId);

	Optional<TrainingEnvironmentJpaEntity> findByAthleteIdAndDefaultEnvironmentTrueAndActiveTrue(UUID athleteId);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("""
			update TrainingEnvironmentJpaEntity e set e.defaultEnvironment = false
			where e.athleteId = :athleteId and e.defaultEnvironment = true
			and (:keepId is null or e.id <> :keepId)
			""")
	void clearDefaultForAthleteExcept(@Param("athleteId") UUID athleteId, @Param("keepId") UUID keepId);

	@Query("""
			select e from TrainingEnvironmentJpaEntity e
			where e.athleteId = :athleteId
			and (:activeOnly = false or e.active = true)
			and (:type is null or e.environmentType = :type)
			and (:equipmentCount = 0 or size(e.availableEquipment) >= :equipmentCount)
			order by e.defaultEnvironment desc, e.name asc, e.id asc
			""")
	Page<TrainingEnvironmentJpaEntity> findFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("type") TrainingEnvironmentType type,
			@Param("equipmentCount") long equipmentCount,
			@Param("activeOnly") boolean activeOnly,
			Pageable pageable);

	List<TrainingEnvironmentJpaEntity> findByAthleteIdAndActiveTrueOrderByDefaultEnvironmentDescNameAscIdAsc(
			UUID athleteId);
}
