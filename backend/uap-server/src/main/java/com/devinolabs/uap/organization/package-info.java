@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"identity :: directory",
				"athlete :: context",
				"athlete :: roster-identity"
		})
package com.devinolabs.uap.organization;
