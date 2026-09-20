package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devinolabs.uap.entitlements.BillingSubjectType;
import com.devinolabs.uap.entitlements.CommercialCapability;
import com.devinolabs.uap.entitlements.CommercialEntitlementRequiredException;
import com.devinolabs.uap.entitlements.EntitlementPort;

class CommercialEntitlementGuardServiceTests {

	private static final UUID ORGANIZATION_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

	private EntitlementEnforcementProperties properties;
	private EntitlementPort entitlementPort;
	private CommercialEntitlementGuardService guard;

	@BeforeEach
	void setUp() {
		properties = new EntitlementEnforcementProperties();
		entitlementPort = mock(EntitlementPort.class);
		guard = new CommercialEntitlementGuardService(properties, entitlementPort);
	}

	@Test
	void disabledDoesNotConsultEntitlementPort() {
		properties.setEnabled(false);

		assertThatCode(() -> guard.requireOrganizationCapability(
				ORGANIZATION_ID, CommercialCapability.ORG_TEAM_MANAGEMENT))
				.doesNotThrowAnyException();
		verifyNoInteractions(entitlementPort);
	}

	@Test
	void enabledAllowsPresentCapability() {
		properties.setEnabled(true);
		when(entitlementPort.hasCapability(
				BillingSubjectType.ORGANIZATION,
				ORGANIZATION_ID,
				CommercialCapability.ORG_TEAM_MANAGEMENT)).thenReturn(true);

		assertThatCode(() -> guard.requireOrganizationCapability(
				ORGANIZATION_ID, CommercialCapability.ORG_TEAM_MANAGEMENT))
				.doesNotThrowAnyException();
	}

	@Test
	void enabledDeniesMissingCapability() {
		properties.setEnabled(true);
		when(entitlementPort.hasCapability(
				BillingSubjectType.ORGANIZATION,
				ORGANIZATION_ID,
				CommercialCapability.ORG_TEAM_MANAGEMENT)).thenReturn(false);

		assertThatThrownBy(() -> guard.requireOrganizationCapability(
				ORGANIZATION_ID, CommercialCapability.ORG_TEAM_MANAGEMENT))
				.isInstanceOf(CommercialEntitlementRequiredException.class)
				.hasMessage("The organization does not currently have access to this capability")
				.hasMessageNotContaining("stripe")
				.hasMessageNotContaining("price_")
				.hasMessageNotContaining(ORGANIZATION_ID.toString());
		assertThat(CommercialEntitlementRequiredException.CODE).isEqualTo("COMMERCIAL_ENTITLEMENT_REQUIRED");
	}

	@Test
	void organizationEdgesCannotRequireIndividualPremiumEvenWhenDisabled() {
		properties.setEnabled(false);

		assertThatThrownBy(() -> guard.requireOrganizationCapability(
				ORGANIZATION_ID, CommercialCapability.INDIVIDUAL_PREMIUM))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("INDIVIDUAL_PREMIUM");
		verifyNoInteractions(entitlementPort);
	}

	@Test
	void nullArgumentsAreRejected() {
		assertThatThrownBy(() -> guard.requireOrganizationCapability(null, CommercialCapability.ORG_TEAM_MANAGEMENT))
				.isInstanceOf(NullPointerException.class);
		assertThatThrownBy(() -> guard.requireOrganizationCapability(ORGANIZATION_ID, null))
				.isInstanceOf(NullPointerException.class);
	}
}
