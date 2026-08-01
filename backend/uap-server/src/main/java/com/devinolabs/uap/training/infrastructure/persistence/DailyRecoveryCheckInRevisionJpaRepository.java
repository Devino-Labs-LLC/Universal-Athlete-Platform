package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface DailyRecoveryCheckInRevisionJpaRepository extends JpaRepository<DailyRecoveryCheckInRevisionJpaEntity, UUID> {

	@EntityGraph(DailyRecoveryCheckInRevisionJpaEntity.WITH_DISCOMFORT)
	List<DailyRecoveryCheckInRevisionJpaEntity> findAllByRecoveryCheckInIdAndAthleteIdOrderByRevisionNumberAsc(
			UUID recoveryCheckInId,
			UUID athleteId);

	int countByRecoveryCheckInId(UUID recoveryCheckInId);

}
