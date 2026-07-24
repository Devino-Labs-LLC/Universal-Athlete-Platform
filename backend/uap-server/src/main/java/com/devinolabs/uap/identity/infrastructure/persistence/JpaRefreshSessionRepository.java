package com.devinolabs.uap.identity.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.identity.application.RefreshSessionRepository;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.RefreshSession;
import com.devinolabs.uap.identity.domain.RefreshSessionId;

@Repository
class JpaRefreshSessionRepository implements RefreshSessionRepository {

	private final RefreshSessionJpaRepository jpaRepository;

	JpaRefreshSessionRepository(RefreshSessionJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public RefreshSession save(RefreshSession session) {
		boolean isNew = !jpaRepository.existsById(session.id().value());
		RefreshSessionJpaEntity saved = jpaRepository.save(RefreshSessionPersistenceMapper.toEntity(session, isNew));
		return RefreshSessionPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<RefreshSession> findById(RefreshSessionId id) {
		return jpaRepository.findById(id.value()).map(RefreshSessionPersistenceMapper::toDomain);
	}

	@Override
	public Optional<RefreshSession> findByTokenDigest(String tokenDigest) {
		return jpaRepository.findByTokenDigest(tokenDigest).map(RefreshSessionPersistenceMapper::toDomain);
	}

	@Override
	public List<RefreshSession> findActiveByAccountId(AccountId accountId, Instant now) {
		return jpaRepository.findActiveByAccountId(accountId.value(), now).stream()
				.map(RefreshSessionPersistenceMapper::toDomain)
				.toList();
	}

}
