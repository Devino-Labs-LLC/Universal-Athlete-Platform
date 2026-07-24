package com.devinolabs.uap.athlete.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.athlete.application.AthleteRepository;
import com.devinolabs.uap.athlete.domain.AccountId;
import com.devinolabs.uap.athlete.domain.Athlete;
import com.devinolabs.uap.athlete.domain.AthleteId;

@Repository
class JpaAthleteRepository implements AthleteRepository {

	private final AthleteJpaRepository jpaRepository;

	JpaAthleteRepository(AthleteJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository, "AthleteJpaRepository must not be null");
	}

	@Override
	public Athlete save(Athlete athlete) {
		boolean isNew = !jpaRepository.existsById(athlete.id().value());
		AthleteJpaEntity saved = jpaRepository.save(AthletePersistenceMapper.toEntity(athlete, isNew));
		return AthletePersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<Athlete> findById(AthleteId id) {
		return jpaRepository.findById(id.value()).map(AthletePersistenceMapper::toDomain);
	}

	@Override
	public Optional<Athlete> findByAccountId(AccountId accountId) {
		return jpaRepository.findByAccountId(accountId.value()).map(AthletePersistenceMapper::toDomain);
	}

	@Override
	public Optional<Athlete> findByAccountIdForUpdate(AccountId accountId) {
		return jpaRepository.findByAccountIdForUpdate(accountId.value()).map(AthletePersistenceMapper::toDomain);
	}

	@Override
	public boolean existsByAccountId(AccountId accountId) {
		return jpaRepository.existsByAccountId(accountId.value());
	}

}
