package com.devinolabs.uap.identity.application;

import java.util.Optional;

import com.devinolabs.uap.identity.domain.EmailVerificationToken;
import com.devinolabs.uap.identity.domain.EmailVerificationTokenId;

public interface EmailVerificationTokenRepository {

	EmailVerificationToken save(EmailVerificationToken token);

	Optional<EmailVerificationToken> findById(EmailVerificationTokenId id);

	Optional<EmailVerificationToken> findByTokenDigest(String tokenDigest);

}
