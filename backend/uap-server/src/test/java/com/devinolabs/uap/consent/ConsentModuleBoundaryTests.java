package com.devinolabs.uap.consent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

import com.devinolabs.uap.UapServerApplication;

class ConsentModuleBoundaryTests {

	@Test
	void consentModuleIsPresentInApplicationModules() {
		ApplicationModules modules = ApplicationModules.of(UapServerApplication.class);

		assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
				.anyMatch(name -> name.equals("consent"));
	}

}
