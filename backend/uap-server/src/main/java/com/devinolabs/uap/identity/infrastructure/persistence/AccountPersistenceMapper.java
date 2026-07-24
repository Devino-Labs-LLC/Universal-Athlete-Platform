package com.devinolabs.uap.identity.infrastructure.persistence;

import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.EmailAddress;
import com.devinolabs.uap.identity.domain.PasswordCredential;

final class AccountPersistenceMapper {

	private AccountPersistenceMapper() {
	}

	static AccountJpaEntity toEntity(Account account, boolean isNew) {
		return new AccountJpaEntity(
				account.id().value(),
				account.email().value(),
				account.passwordCredential().hash(),
				account.status(),
				account.failedLoginAttempts(),
				account.lockedUntil(),
				account.emailVerifiedAt(),
				account.createdAt(),
				account.updatedAt(),
				account.version(),
				isNew);
	}

	static Account toDomain(AccountJpaEntity entity) {
		return Account.rehydrate(
				AccountId.of(entity.getId()),
				EmailAddress.of(entity.getEmail()),
				PasswordCredential.fromHash(entity.getPasswordHash()),
				entity.getStatus(),
				entity.getFailedLoginAttempts(),
				entity.getLockedUntil(),
				entity.getEmailVerifiedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}

}
