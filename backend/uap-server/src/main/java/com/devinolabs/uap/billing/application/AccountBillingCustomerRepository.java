package com.devinolabs.uap.billing.application;

import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.billing.domain.AccountBillingCustomer;

public interface AccountBillingCustomerRepository {

	AccountBillingCustomer save(AccountBillingCustomer customer);

	Optional<AccountBillingCustomer> findByAccountId(UUID accountId);

	Optional<AccountBillingCustomer> findByAccountIdForUpdate(UUID accountId);

}
