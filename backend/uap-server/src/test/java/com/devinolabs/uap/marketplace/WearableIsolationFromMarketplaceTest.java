package com.devinolabs.uap.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.integrations.application.DeviceService;
import com.devinolabs.uap.integrations.application.SensorFusionService;
import com.devinolabs.uap.marketplace.application.MarketplaceCatalogService;
import com.devinolabs.uap.marketplace.application.MarketplaceDiscoveryService;
import com.devinolabs.uap.marketplace.application.MarketplaceReportingService;

class WearableIsolationFromMarketplaceTest {

	@Test
	void marketplaceServicesDoNotDependOnWearableInternals() {
		assertIsolated(MarketplaceDiscoveryService.class);
		assertIsolated(MarketplaceCatalogService.class);
		assertIsolated(MarketplaceReportingService.class);
	}

	private static void assertIsolated(Class<?> type) {
		assertThat(Arrays.stream(type.getDeclaredFields()).map(Field::getType))
				.doesNotContain(DeviceService.class, SensorFusionService.class);
		assertThat(type.getPackageName()).doesNotContain("integrations");
	}
}
