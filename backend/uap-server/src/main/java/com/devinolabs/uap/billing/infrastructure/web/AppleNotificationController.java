package com.devinolabs.uap.billing.infrastructure.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.billing.application.AppleNotificationService;
import com.devinolabs.uap.billing.application.InvalidApplePurchaseException;

@RestController
@RequestMapping("/api/v1/billing/webhooks/apple")
@ConditionalOnProperty(prefix = "uap.billing.apple", name = "enabled", havingValue = "true")
class AppleNotificationController {

	private final AppleNotificationService notificationService;

	AppleNotificationController(AppleNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@PostMapping
	ResponseEntity<Void> handle(@RequestBody byte[] payload) {
		try {
			notificationService.handle(payload);
			return ResponseEntity.ok().build();
		}
		catch (InvalidApplePurchaseException ex) {
			return ResponseEntity.badRequest().build();
		}
	}

}
