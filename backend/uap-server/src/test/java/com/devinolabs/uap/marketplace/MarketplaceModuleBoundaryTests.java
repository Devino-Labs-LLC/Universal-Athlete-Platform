package com.devinolabs.uap.marketplace;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.devinolabs.uap.UapServerApplication;

class MarketplaceModuleBoundaryTests {

	@Test
	void marketplaceModuleIsPresentAndVerifies() {
		ApplicationModules modules = ApplicationModules.of(UapServerApplication.class);
		assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
				.contains("marketplace");
		modules.verify();
	}
}
