package com.devinolabs.uap.identity.application;

import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.domain.AccessTokenIssuer;
import com.devinolabs.uap.identity.domain.IssuedAccessToken;
import com.devinolabs.uap.identity.domain.IssuedRefreshSession;
import com.devinolabs.uap.identity.domain.RefreshSession;
import com.devinolabs.uap.identity.domain.RefreshSessionRevocationReason;
import com.devinolabs.uap.identity.domain.TokenDigester;

@Service
public class RotateRefreshSessionUseCase {

	private final RefreshSessionRepository refreshSessionRepository;
	private final AccessTokenIssuer accessTokenIssuer;
	private final TokenDigester tokenDigester;
	private final Duration refreshTokenTtl;
	private final IdentityTransactionService identityTransactionService;
	private final Clock clock;

	public RotateRefreshSessionUseCase(
			RefreshSessionRepository refreshSessionRepository,
			AccessTokenIssuer accessTokenIssuer,
			TokenDigester tokenDigester,
			Duration refreshTokenTtl,
			IdentityTransactionService identityTransactionService,
			Clock clock) {
		this.refreshSessionRepository = Objects.requireNonNull(refreshSessionRepository);
		this.accessTokenIssuer = Objects.requireNonNull(accessTokenIssuer);
		this.tokenDigester = Objects.requireNonNull(tokenDigester);
		this.refreshTokenTtl = Objects.requireNonNull(refreshTokenTtl);
		this.identityTransactionService = Objects.requireNonNull(identityTransactionService);
		this.clock = Objects.requireNonNull(clock);
	}

	/**
	 * Reuse-detection strategy: refresh sessions are treated as an account-scoped
	 * family. Presenting a revoked/rotated refresh token revokes every active
	 * refresh session for that account, invalidating the entire replacement chain.
	 */
	@Transactional
	public AuthenticationResult rotate(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}

		String digest = tokenDigester.digest(rawRefreshToken);
		RefreshSession session = refreshSessionRepository.findByTokenDigest(digest)
				.orElseThrow(InvalidRefreshTokenException::new);

		if (session.isRevoked()) {
			identityTransactionService.revokeAccountSessionFamily(session.id());
			throw new RevokedRefreshTokenException();
		}
		if (session.isExpired(clock)) {
			identityTransactionService.revokeSession(session.id(), RefreshSessionRevocationReason.LOGOUT);
			throw new ExpiredRefreshTokenException();
		}

		IssuedRefreshSession replacement = RefreshSession.issue(
				session.accountId(),
				tokenDigester,
				refreshTokenTtl,
				clock);
		refreshSessionRepository.save(replacement.session());

		session.rotateTo(replacement.session().id(), clock);
		session.markUsed(clock);
		refreshSessionRepository.save(session);

		IssuedAccessToken accessToken = accessTokenIssuer.issue(session.accountId());
		return new AuthenticationResult(
				session.accountId(),
				accessToken,
				replacement.rawToken(),
				replacement.session().id());
	}

}
