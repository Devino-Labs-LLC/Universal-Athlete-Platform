@org.springframework.modulith.ApplicationModule(
		allowedDependencies = {
				"identity :: auth",
				"athlete :: context",
				"training :: coach-context",
				"integrations :: evidence",
				"integrations :: connections"
		})
package com.devinolabs.uap.aicoach;
