package com.devinolabs.uap.identity.api;

import java.util.Optional;
import java.util.UUID;

/**
 * Account directory lookups for invitation binding and accept authorization.
 */
public interface AccountDirectoryPort {

	Optional<AccountDirectoryRef> findByNormalizedEmail(String email);

	Optional<AccountDirectoryRef> findByAccountId(UUID accountId);

}
