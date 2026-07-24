package com.devinolabs.uap;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

	@Test
	void verifiesModularStructure() {
		ApplicationModules.of(UapServerApplication.class).verify();
	}

}
