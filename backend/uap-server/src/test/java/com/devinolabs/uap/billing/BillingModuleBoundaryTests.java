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
				.anyMatch(name -> name.equals("billing"));
		modules.verify();
	}

	@Test
	void entitlementsNamedInterfaceIsPublished() {
		ApplicationModules modules = ApplicationModules.of(UapServerApplication.class);

		assertThat(modules.getModuleByName("billing"))
				.isPresent()
				.get()
				.satisfies(module -> assertThat(module.getNamedInterfaces().stream()
						.map(named -> named.getName())
						.toList()).anyMatch(name -> name.equals("entitlements")));
	}

}
