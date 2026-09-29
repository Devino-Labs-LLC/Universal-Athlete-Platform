package com.devinolabs.uap.billing.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

record ValidateGooglePlayPurchaseRequest(
		@NotBlank(message = "purchaseToken is required") String purchaseToken,
		@NotBlank(message = "productId is required") String productId) {
}
