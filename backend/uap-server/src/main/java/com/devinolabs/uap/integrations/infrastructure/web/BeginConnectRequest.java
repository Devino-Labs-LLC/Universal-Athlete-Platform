package com.devinolabs.uap.integrations.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import com.devinolabs.uap.integrations.domain.HealthProviderKey;

record BeginConnectRequest(
		@NotNull HealthProviderKey provider,
		@NotNull UUID requestId) {
}
