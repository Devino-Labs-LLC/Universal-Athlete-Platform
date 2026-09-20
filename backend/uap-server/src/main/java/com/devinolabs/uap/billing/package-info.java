@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"organization :: membership",
				"audit :: writer",
				"entitlements"
		})
package com.devinolabs.uap.billing;
