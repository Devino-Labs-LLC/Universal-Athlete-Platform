@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"organization :: membership",
				"audit :: writer"
		})
package com.devinolabs.uap.billing;
