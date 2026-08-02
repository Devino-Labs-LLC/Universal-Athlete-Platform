package com.devinolabs.uap.training.infrastructure.web;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.domain.AccountId;

final class RecoveryAnalyticsWebSupport {

	private RecoveryAnalyticsWebSupport() {
	}

	static AccountId accountId(org.springframework.security.core.Authentication authentication) {
		AccountPrincipal principal = (AccountPrincipal) authentication.getPrincipal();
		return AccountId.of(principal.accountUuid());
	}

}
