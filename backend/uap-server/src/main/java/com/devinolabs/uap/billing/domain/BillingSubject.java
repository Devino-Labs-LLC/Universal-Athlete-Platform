package com.devinolabs.uap.billing.domain;

import java.util.Objects;
import java.util.UUID;

import com.devinolabs.uap.billing.api.BillingSubjectType;

/**
 * Commercial subject: Account or Organization identity by UUID value only.
 */
public record BillingSubject(BillingSubjectType type, UUID subjectId) {

	public BillingSubject {
		Objects.requireNonNull(type, "type must not be null");
		Objects.requireNonNull(subjectId, "subjectId must not be null");
	}

	public static BillingSubject account(UUID accountId) {
		return new BillingSubject(BillingSubjectType.ACCOUNT, accountId);
	}

	public static BillingSubject organization(UUID organizationId) {
		return new BillingSubject(BillingSubjectType.ORGANIZATION, organizationId);
	}

}
