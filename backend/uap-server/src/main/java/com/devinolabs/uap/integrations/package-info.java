@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"athlete :: context",
				"audit :: writer"
		})
package com.devinolabs.uap.integrations;
