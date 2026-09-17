package com.devinolabs.uap.billing.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record SynchronizeOrganizationSubscriptionRequest(
		@NotBlank @Size(max = 255) String checkoutSessionId) {
}
