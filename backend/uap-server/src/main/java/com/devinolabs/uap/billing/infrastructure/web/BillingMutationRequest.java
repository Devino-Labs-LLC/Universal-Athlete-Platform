package com.devinolabs.uap.billing.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

record BillingMutationRequest(@NotNull UUID requestId) {
}
