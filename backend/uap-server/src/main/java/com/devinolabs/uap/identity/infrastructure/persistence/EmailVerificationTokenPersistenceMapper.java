package com.devinolabs.uap.identity.infrastructure.persistence;

import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.EmailVerificationToken;
import com.devinolabs.uap.identity.domain.EmailVerificationTokenId;

final class EmailVerificationTokenPersistenceMapper {

	private EmailVerificationTokenPersistenceMapper() {
	}

	static EmailVerificationTokenJpaEntity toEntity(EmailVerificationToken token, boolean isNew) {
		return new EmailVerificationTokenJpaEntity(
				token.id().value(),
				token.accountId().value(),
				token.tokenDigest(),
				token.createdAt(),
				token.expiresAt(),
				token.consumedAt(),
				token.version(),
				isNew);
	}

	static EmailVerificationToken toDomain(EmailVerificationTokenJpaEntity entity) {
		return EmailVerificationToken.rehydrate(
				EmailVerificationTokenId.of(entity.getId()),
				AccountId.of(entity.getAccountId()),
				entity.getTokenDigest(),
				entity.getCreatedAt(),
				entity.getExpiresAt(),
				entity.getConsumedAt(),
				entity.getVersion());
	}

}
