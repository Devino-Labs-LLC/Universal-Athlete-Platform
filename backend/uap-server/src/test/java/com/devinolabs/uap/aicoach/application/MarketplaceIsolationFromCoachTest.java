package com.devinolabs.uap.aicoach.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.devinolabs.uap.marketplace.application.MarketplaceCatalogService;
import com.devinolabs.uap.marketplace.application.MarketplaceDiscoveryService;
import com.devinolabs.uap.marketplace.application.MarketplaceReportingService;
import com.devinolabs.uap.training.domain.ReadinessCalculator;

class MarketplaceIsolationFromCoachTest {

	@Test
	void coachContextAndPromptPathHaveNoMarketplaceFieldsOrDependencies() {
		assertThat(Arrays.stream(CoachContext.class.getDeclaredFields()).map(Field::getName))
				.noneMatch(name -> name.toLowerCase().contains("market")
						|| name.toLowerCase().contains("listing")
						|| name.toLowerCase().contains("promo"));
		assertThat(CoachContextBuilder.class.getDeclaredFields())
				.extracting(Field::getType)
				.doesNotContain(
						MarketplaceCatalogService.class,
						MarketplaceDiscoveryService.class,
						MarketplaceReportingService.class);
		assertThat(CoachPromptComposer.class.getDeclaredFields())
				.extracting(Field::getType)
				.doesNotContain(MarketplaceDiscoveryService.class);
	}

	@Test
	void readinessCalculatorDoesNotDependOnMarketplace() {
		assertThat(ReadinessCalculator.class.getDeclaredFields())
				.extracting(Field::getType)
				.noneMatch(type -> type.getPackageName().startsWith("com.devinolabs.uap.marketplace"));
		assertThat(ReadinessCalculator.class.getDeclaredMethods())
				.extracting(method -> method.getReturnType().getSimpleName())
				.noneMatch(name -> name.contains("Listing") || name.contains("Marketplace"));
	}
}
