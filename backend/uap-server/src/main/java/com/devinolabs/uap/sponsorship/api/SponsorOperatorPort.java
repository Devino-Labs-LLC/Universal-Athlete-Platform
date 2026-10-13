package com.devinolabs.uap.sponsorship.api;

import java.util.UUID;

public interface SponsorOperatorPort {

	void requireOperator(UUID accountId);

	boolean isOperator(UUID accountId);
}
