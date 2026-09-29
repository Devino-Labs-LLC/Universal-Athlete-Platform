package com.devinolabs.uap.billing.infrastructure.web;

import jakarta.validation.constraints.NotBlank;

record ValidateAppleTransactionRequest(
		@NotBlank(message = "signedTransactionInfo is required") String signedTransactionInfo) {
}
