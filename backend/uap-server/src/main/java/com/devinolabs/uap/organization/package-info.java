@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"identity :: directory",
				"athlete :: context",
				"athlete :: roster-identity",
				"audit :: writer"
		})
package com.devinolabs.uap.organization;
