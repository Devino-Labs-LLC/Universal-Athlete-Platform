@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"organization :: membership",
				"sponsorship :: catalog"
		})
package com.devinolabs.uap.marketplace;
