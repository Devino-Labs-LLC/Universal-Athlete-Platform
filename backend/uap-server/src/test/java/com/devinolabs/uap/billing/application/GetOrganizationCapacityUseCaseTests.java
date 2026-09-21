package com.devinolabs.uap.billing.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.devinolabs.uap.entitlements.OrganizationAthleteBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.Ambiguous;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.EffectiveBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacity.NoEffectiveBand;
import com.devinolabs.uap.entitlements.OrganizationCommercialCapacityPort;
import com.devinolabs.uap.organization.api.OrganizationMembershipPort;

class GetOrganizationCapacityUseCaseTests {

	private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
	private static final UUID ORGANIZATION_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

	private OrganizationMembershipPort membershipPort;
	private OrganizationCommercialCapacityPort capacityPort;
	private GetOrganizationCapacityUseCase useCase;

	@BeforeEach
	void setUp() {
		membershipPort = mock(OrganizationMembershipPort.class);
		capacityPort = mock(OrganizationCommercialCapacityPort.class);
		useCase = new GetOrganizationCapacityUseCase(membershipPort, capacityPort);
	}

	@Test
	void nonOwnerIsNotFoundWithoutUsage() {
		when(membershipPort.canManageOrganization(ACTOR, ORGANIZATION_ID)).thenReturn(false);

		assertThatThrownBy(() -> useCase.execute(ACTOR, ORGANIZATION_ID))
				.isInstanceOf(BillingOrganizationNotFoundException.class);
	}

	@Test
	void effectiveBandReportsNumericSnapshot() {
		when(membershipPort.canManageOrganization(ACTOR, ORGANIZATION_ID)).thenReturn(true);
		when(membershipPort.countDistinctActiveAthletes(ORGANIZATION_ID)).thenReturn(23L);
		when(capacityPort.resolve(ORGANIZATION_ID)).thenReturn(new EffectiveBand(OrganizationAthleteBand.BAND_25));

		OrganizationCapacitySnapshot snapshot = useCase.execute(ACTOR, ORGANIZATION_ID);

		assertThat(snapshot.activeAthleteCount()).isEqualTo(23);
		assertThat(snapshot.bandCapacity()).isEqualTo(25);
		assertThat(snapshot.remainingCapacity()).isEqualTo(2);
		assertThat(snapshot.atCapacity()).isFalse();
		assertThat(snapshot.overCapacity()).isFalse();
	}

	@Test
	void overCapacityIsTruthyWhenCountExceedsBand() {
		when(membershipPort.canManageOrganization(ACTOR, ORGANIZATION_ID)).thenReturn(true);
		when(membershipPort.countDistinctActiveAthletes(ORGANIZATION_ID)).thenReturn(40L);
		when(capacityPort.resolve(ORGANIZATION_ID)).thenReturn(new EffectiveBand(OrganizationAthleteBand.BAND_25));

		OrganizationCapacitySnapshot snapshot = useCase.execute(ACTOR, ORGANIZATION_ID);

		assertThat(snapshot.remainingCapacity()).isZero();
		assertThat(snapshot.atCapacity()).isFalse();
		assertThat(snapshot.overCapacity()).isTrue();
	}

	@Test
	void noEffectiveBandDoesNotFabricateZeroCapacity() {
		when(membershipPort.canManageOrganization(ACTOR, ORGANIZATION_ID)).thenReturn(true);
		when(membershipPort.countDistinctActiveAthletes(ORGANIZATION_ID)).thenReturn(12L);
		when(capacityPort.resolve(ORGANIZATION_ID)).thenReturn(new NoEffectiveBand());

		OrganizationCapacitySnapshot snapshot = useCase.execute(ACTOR, ORGANIZATION_ID);

		assertThat(snapshot.activeAthleteCount()).isEqualTo(12);
		assertThat(snapshot.bandCapacity()).isNull();
		assertThat(snapshot.remainingCapacity()).isNull();
		assertThat(snapshot.atCapacity()).isFalse();
		assertThat(snapshot.overCapacity()).isFalse();
	}

	@Test
	void ambiguousStateKeepsCountAndOmitsBand() {
		when(membershipPort.canManageOrganization(ACTOR, ORGANIZATION_ID)).thenReturn(true);
		when(membershipPort.countDistinctActiveAthletes(ORGANIZATION_ID)).thenReturn(3L);
		when(capacityPort.resolve(ORGANIZATION_ID)).thenReturn(new Ambiguous());

		OrganizationCapacitySnapshot snapshot = useCase.execute(ACTOR, ORGANIZATION_ID);

		assertThat(snapshot.activeAthleteCount()).isEqualTo(3);
		assertThat(snapshot.bandCapacity()).isNull();
		assertThat(snapshot.remainingCapacity()).isNull();
	}
}
