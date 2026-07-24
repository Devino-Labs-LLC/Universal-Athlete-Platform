package com.devinolabs.uap.identity.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.RefreshSession;
import com.devinolabs.uap.identity.domain.RefreshSessionRevocationReason;
import com.devinolabs.uap.identity.domain.TokenDigester;

@Service
public class LogoutUseCase {

	private final RefreshSessionRepository refreshSessionRepository;
	private final TokenDigester tokenDigester;
	private final Clock clock;

	public LogoutUseCase(
			RefreshSessionRepository refreshSessionRepository,
			TokenDigester tokenDigester,
			Clock clock) {
		this.refreshSessionRepository = Objects.requireNonNull(refreshSessionRepository);
		this.tokenDigester = Objects.requireNonNull(tokenDigester);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional
	public void logout(String rawRefreshToken) {
		if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
			throw new InvalidRefreshTokenException();
		}

		RefreshSession session = refreshSessionRepository.findByTokenDigest(tokenDigester.digest(rawRefreshToken))
				.orElseThrow(InvalidRefreshTokenException::new);
		session.revoke(RefreshSessionRevocationReason.LOGOUT, clock);
		refreshSessionRepository.save(session);
	}

	@Transactional
	public void logoutAll(AccountId accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		List<RefreshSession> activeSessions = refreshSessionRepository.findActiveByAccountId(accountId, clock.instant());
		for (RefreshSession session : activeSessions) {
			session.revoke(RefreshSessionRevocationReason.REVOKE_ALL, clock);
			refreshSessionRepository.save(session);
		}
	}

}
