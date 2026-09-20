@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"identity :: directory",
				"athlete :: context",
				"athlete :: roster-identity",
				"audit :: writer",
				"entitlements"
		})
package com.devinolabs.uap.organization;
