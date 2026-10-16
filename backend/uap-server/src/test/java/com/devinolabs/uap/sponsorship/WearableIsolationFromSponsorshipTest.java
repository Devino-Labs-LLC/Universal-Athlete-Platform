package com.devinolabs.uap.sponsorship;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.integrations.application.DeviceService;
import com.devinolabs.uap.integrations.application.SensorFusionService;
import com.devinolabs.uap.sponsorship.application.PlacementService;
import com.devinolabs.uap.sponsorship.application.SponsorshipCatalogService;
import com.devinolabs.uap.sponsorship.application.SponsorshipReportingService;

class WearableIsolationFromSponsorshipTest {

	@Test
	void sponsorshipDoesNotDependOnWearableInternals() {
		assertIsolated(PlacementService.class);
		assertIsolated(SponsorshipCatalogService.class);
		assertIsolated(SponsorshipReportingService.class);
	}

	private static void assertIsolated(Class<?> type) {
		assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getType))
				.doesNotContain(DeviceService.class, SensorFusionService.class);
	}
}
