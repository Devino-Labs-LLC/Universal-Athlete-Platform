package com.devinolabs.uap.billing;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.devinolabs.uap.UapServerApplication;

class BillingModuleBoundaryTests {

	@Test
	void billingModuleIsPresentAndVerifies() {
		ApplicationModules modules = ApplicationModules.of(UapServerApplication.class);

		assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
				.contains("billing", "entitlements");
		modules.verify();
	}

	@Test
	void organizationAndTrainingDependOnEntitlementsModuleNotBillingInternals() {
		ApplicationModules modules = ApplicationModules.of(UapServerApplication.class);
		assertThat(modules.getModuleByName("organization")).isPresent();
		assertThat(modules.getModuleByName("training")).isPresent();
		assertThat(modules.getModuleByName("entitlements")).isPresent();
		modules.verify();
	}

}
