package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.devinolabs.uap.athlete.domain.MeasurementSource;
import com.devinolabs.uap.athlete.domain.MeasurementType;

interface AthleteMeasurementJpaRepository extends JpaRepository<AthleteMeasurementJpaEntity, UUID> {

	Optional<AthleteMeasurementJpaEntity> findByIdAndAthleteId(UUID id, UUID athleteId);

	@Query("""
			select m from AthleteMeasurementJpaEntity m
			where m.athleteId = :athleteId
			and (:measurementType is null or m.measurementType = :measurementType)
			and (:source is null or m.source = :source)
			and (:athleteSportId is null or m.athleteSportId = :athleteSportId)
			and (:athleteGoalId is null or m.athleteGoalId = :athleteGoalId)
			and (:measuredFrom is null or m.measuredAt >= :measuredFrom)
			and (:measuredTo is null or m.measuredAt <= :measuredTo)
			order by m.measuredAt desc, m.createdAt desc, m.id asc
			""")
	List<AthleteMeasurementJpaEntity> findFiltered(
			@Param("athleteId") UUID athleteId,
			@Param("measurementType") MeasurementType measurementType,
			@Param("source") MeasurementSource source,
			@Param("athleteSportId") UUID athleteSportId,
			@Param("athleteGoalId") UUID athleteGoalId,
			@Param("measuredFrom") Instant measuredFrom,
			@Param("measuredTo") Instant measuredTo);

}
