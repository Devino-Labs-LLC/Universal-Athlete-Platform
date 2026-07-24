package com.devinolabs.uap.identity.infrastructure.persistence;

import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.identity.application.EmailVerificationTokenRepository;
import com.devinolabs.uap.identity.domain.EmailVerificationToken;
import com.devinolabs.uap.identity.domain.EmailVerificationTokenId;

@Repository
class JpaEmailVerificationTokenRepository implements EmailVerificationTokenRepository {

	private final EmailVerificationTokenJpaRepository jpaRepository;

	JpaEmailVerificationTokenRepository(EmailVerificationTokenJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public EmailVerificationToken save(EmailVerificationToken token) {
		boolean isNew = !jpaRepository.existsById(token.id().value());
		EmailVerificationTokenJpaEntity saved = jpaRepository
				.save(EmailVerificationTokenPersistenceMapper.toEntity(token, isNew));
		return EmailVerificationTokenPersistenceMapper.toDomain(saved);
	}

	@Override
	public Optional<EmailVerificationToken> findById(EmailVerificationTokenId id) {
		return jpaRepository.findById(id.value()).map(EmailVerificationTokenPersistenceMapper::toDomain);
	}

	@Override
	public Optional<EmailVerificationToken> findByTokenDigest(String tokenDigest) {
		return jpaRepository.findByTokenDigest(tokenDigest).map(EmailVerificationTokenPersistenceMapper::toDomain);
	}

}
