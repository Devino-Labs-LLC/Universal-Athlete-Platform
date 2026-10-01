package com.devinolabs.uap.integrations;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.devinolabs.uap.UapServerApplication;

class IntegrationsModuleBoundaryTests {

	@Test
	void integrationsModuleIsPresentAndVerifies() {
		ApplicationModules modules = ApplicationModules.of(UapServerApplication.class);

		assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
				.contains("integrations");
		assertThat(modules.getModuleByName("integrations")).isPresent();
		modules.verify();
	}

}
