package com.devinolabs.uap.identity.application;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.devinolabs.uap.identity.domain.AccountId;
import com.devinolabs.uap.identity.domain.RefreshSession;
import com.devinolabs.uap.identity.domain.RefreshSessionId;

public interface RefreshSessionRepository {

	RefreshSession save(RefreshSession session);

	Optional<RefreshSession> findById(RefreshSessionId id);

	Optional<RefreshSession> findByTokenDigest(String tokenDigest);

	List<RefreshSession> findActiveByAccountId(AccountId accountId, Instant now);

}
