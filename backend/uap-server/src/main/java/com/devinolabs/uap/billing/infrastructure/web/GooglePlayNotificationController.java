package com.devinolabs.uap.billing.infrastructure.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.billing.application.GooglePlayNotificationService;
import com.devinolabs.uap.billing.application.InvalidGooglePlayPurchaseException;

@RestController
@RequestMapping("/api/v1/billing/webhooks/google-play")
@ConditionalOnProperty(prefix = "uap.billing.google-play", name = "enabled", havingValue = "true")
class GooglePlayNotificationController {

	private final GooglePlayNotificationService notificationService;

	GooglePlayNotificationController(GooglePlayNotificationService notificationService) {
		this.notificationService = notificationService;
	}

	@PostMapping
	ResponseEntity<Void> handle(@RequestBody byte[] payload) {
		try {
			notificationService.handle(payload);
			return ResponseEntity.ok().build();
		}
		catch (InvalidGooglePlayPurchaseException ex) {
			return ResponseEntity.badRequest().build();
		}
	}

}
