package com.devinolabs.uap.training.infrastructure.persistence;

import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.training.application.DailyRecoveryCheckInRevisionRepository;
import com.devinolabs.uap.training.domain.AthleteId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInId;
import com.devinolabs.uap.training.domain.DailyRecoveryCheckInRevision;

@Repository
class JpaDailyRecoveryCheckInRevisionRepository implements DailyRecoveryCheckInRevisionRepository {

	private final DailyRecoveryCheckInRevisionJpaRepository jpaRepository;

	JpaDailyRecoveryCheckInRevisionRepository(DailyRecoveryCheckInRevisionJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public DailyRecoveryCheckInRevision save(DailyRecoveryCheckInRevision revision) {
		DailyRecoveryCheckInRevisionJpaEntity saved = jpaRepository
				.save(DailyRecoveryCheckInPersistenceMapper.toRevisionEntity(revision));
		return DailyRecoveryCheckInPersistenceMapper.toRevisionDomain(saved);
	}

	@Override
	public List<DailyRecoveryCheckInRevision> findAllByCheckInIdAndAthleteIdOrderByRevisionNumber(
			DailyRecoveryCheckInId checkInId,
			AthleteId athleteId) {
		return jpaRepository
				.findAllByRecoveryCheckInIdAndAthleteIdOrderByRevisionNumberAsc(
						checkInId.value(), athleteId.value())
				.stream()
				.map(DailyRecoveryCheckInPersistenceMapper::toRevisionDomain)
				.toList();
	}

	@Override
	public int countByCheckInId(DailyRecoveryCheckInId checkInId) {
		return jpaRepository.countByRecoveryCheckInId(checkInId.value());
	}

}
