@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"athlete :: context",
				"athlete :: roster-identity",
				"organization :: membership",
				"consent :: grants",
				"audit :: writer",
				"entitlements"
		})
package com.devinolabs.uap.training;
