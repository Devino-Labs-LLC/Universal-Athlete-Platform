package com.devinolabs.uap.billing.infrastructure.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.billing.application.InvalidWebhookSignatureException;
import com.devinolabs.uap.billing.application.OrganizationWebhookService;

@RestController
@RequestMapping("/api/v1/billing/webhooks/stripe")
@ConditionalOnProperty(prefix = "uap.billing.stripe", name = "enabled", havingValue = "true")
class StripeWebhookController {

	private final OrganizationWebhookService webhookService;

	StripeWebhookController(OrganizationWebhookService webhookService) {
		this.webhookService = webhookService;
	}

	@PostMapping
	ResponseEntity<Void> handle(
			@RequestBody byte[] payload,
			@RequestHeader(value = "Stripe-Signature", required = false) String signature) {
		try {
			webhookService.handle(payload, signature);
			return ResponseEntity.ok().build();
		}
		catch (InvalidWebhookSignatureException ex) {
			return ResponseEntity.badRequest().build();
		}
	}

}
