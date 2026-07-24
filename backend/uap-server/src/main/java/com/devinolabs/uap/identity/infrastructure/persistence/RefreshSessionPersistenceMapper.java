package com.devinolabs.uap.identity.infrastructure.persistence;

import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.RefreshSession;
import com.devinolabs.uap.identity.domain.RefreshSessionId;

final class RefreshSessionPersistenceMapper {

	private RefreshSessionPersistenceMapper() {
	}

	static RefreshSessionJpaEntity toEntity(RefreshSession session, boolean isNew) {
		return new RefreshSessionJpaEntity(
				session.id().value(),
				session.accountId().value(),
				session.tokenDigest(),
				session.createdAt(),
				session.expiresAt(),
				session.lastUsedAt(),
				session.revokedAt(),
				session.replacedBySessionId() == null ? null : session.replacedBySessionId().value(),
				session.revocationReason(),
				session.version(),
				isNew);
	}

	static RefreshSession toDomain(RefreshSessionJpaEntity entity) {
		return RefreshSession.rehydrate(
				RefreshSessionId.of(entity.getId()),
				AccountId.of(entity.getAccountId()),
				entity.getTokenDigest(),
				entity.getCreatedAt(),
				entity.getExpiresAt(),
				entity.getLastUsedAt(),
				entity.getRevokedAt(),
				entity.getReplacedBySessionId() == null ? null : RefreshSessionId.of(entity.getReplacedBySessionId()),
				entity.getRevocationReason(),
				entity.getVersion());
	}

}
