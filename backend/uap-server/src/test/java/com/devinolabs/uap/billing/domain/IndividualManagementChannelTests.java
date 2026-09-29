package com.devinolabs.uap.billing.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IndividualManagementChannelTests {

	@Test
	void forProviderMapsEachIndividualOrigin() {
		assertThat(IndividualManagementChannel.forProvider(BillingProvider.STRIPE))
				.isEqualTo(IndividualManagementChannel.STRIPE_CUSTOMER_PORTAL);
		assertThat(IndividualManagementChannel.forProvider(BillingProvider.APPLE_APP_STORE))
				.isEqualTo(IndividualManagementChannel.APPLE_APP_STORE);
		assertThat(IndividualManagementChannel.forProvider(BillingProvider.GOOGLE_PLAY))
				.isEqualTo(IndividualManagementChannel.GOOGLE_PLAY);
	}

}
