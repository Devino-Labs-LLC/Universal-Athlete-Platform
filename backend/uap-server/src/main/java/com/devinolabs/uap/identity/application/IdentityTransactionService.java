package com.devinolabs.uap.identity.application;

import java.time.Clock;
import java.util.List;
import java.util.Objects;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.identity.domain.Account;
import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.LockoutPolicy;
import com.devinolabs.uap.identity.domain.RefreshSession;
import com.devinolabs.uap.identity.domain.RefreshSessionId;
import com.devinolabs.uap.identity.domain.RefreshSessionRevocationReason;

@Service
public class IdentityTransactionService {

	private static final int MAX_OPTIMISTIC_LOCK_RETRIES = 3;

	private final AccountRepository accountRepository;
	private final RefreshSessionRepository refreshSessionRepository;
	private final LockoutPolicy lockoutPolicy;
	private final Clock clock;

	public IdentityTransactionService(
			AccountRepository accountRepository,
			RefreshSessionRepository refreshSessionRepository,
			LockoutPolicy lockoutPolicy,
			Clock clock) {
		this.accountRepository = Objects.requireNonNull(accountRepository);
		this.refreshSessionRepository = Objects.requireNonNull(refreshSessionRepository);
		this.lockoutPolicy = Objects.requireNonNull(lockoutPolicy);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public Account recordFailedAuthentication(AccountId accountId) {
		Objects.requireNonNull(accountId, "accountId must not be null");
		int attempts = 0;
		while (true) {
			try {
				Account account = accountRepository.findById(accountId)
						.orElseThrow(() -> new IllegalStateException("Account not found for failed authentication"));
				account.recordFailedAuthentication(lockoutPolicy, clock);
				return accountRepository.save(account);
			}
			catch (ObjectOptimisticLockingFailureException ex) {
				attempts++;
				if (attempts >= MAX_OPTIMISTIC_LOCK_RETRIES) {
					throw ex;
				}
			}
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void revokeAccountSessionFamily(RefreshSessionId reusedSessionId) {
		Objects.requireNonNull(reusedSessionId, "reusedSessionId must not be null");
		RefreshSession reusedSession = refreshSessionRepository.findById(reusedSessionId)
				.orElseThrow(() -> new IllegalStateException("Refresh session not found for reuse detection"));

		reusedSession.revoke(RefreshSessionRevocationReason.REUSE_DETECTED, clock);
		refreshSessionRepository.save(reusedSession);

		List<RefreshSession> activeSessions = refreshSessionRepository
				.findActiveByAccountId(reusedSession.accountId(), clock.instant());
		for (RefreshSession activeSession : activeSessions) {
			if (activeSession.id().equals(reusedSession.id())) {
				continue;
			}
			activeSession.revoke(RefreshSessionRevocationReason.REUSE_DETECTED, clock);
			refreshSessionRepository.save(activeSession);
		}
	}

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void revokeSession(RefreshSessionId sessionId, RefreshSessionRevocationReason reason) {
		Objects.requireNonNull(sessionId, "sessionId must not be null");
		Objects.requireNonNull(reason, "reason must not be null");
		refreshSessionRepository.findById(sessionId).ifPresent(session -> {
			session.revoke(reason, clock);
			refreshSessionRepository.save(session);
		});
	}

}
