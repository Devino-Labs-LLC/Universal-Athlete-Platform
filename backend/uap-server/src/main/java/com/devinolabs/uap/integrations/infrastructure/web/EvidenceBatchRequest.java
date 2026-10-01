package com.devinolabs.uap.integrations.infrastructure.web;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record EvidenceBatchRequest(
		@NotNull UUID requestId,
		@NotNull @NotEmpty @Valid List<EvidenceBatchItemRequest> items) {
}

record EvidenceBatchItemRequest(
		@NotBlank @Size(max = 191) String externalRecordId,
		@NotBlank String signalFamily,
		@NotBlank @Size(max = 80) String signalType,
		BigDecimal valueNumeric,
		@Size(max = 512) String valueText,
		@NotBlank @Size(max = 32) String unitCode,
		Instant periodStart,
		Instant periodEnd,
		@NotNull Instant observedAt,
		Instant providerUpdatedAt,
		@Size(max = 128) String sourceDeviceOrApp,
		String provenanceClass) {
}
