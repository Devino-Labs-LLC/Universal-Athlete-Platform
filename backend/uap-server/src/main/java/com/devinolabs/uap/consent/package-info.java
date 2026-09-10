@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"athlete :: context",
				"organization :: membership",
				"audit :: writer"
		})
package com.devinolabs.uap.consent;
