package com.devinolabs.uap.integrations.infrastructure.web;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

record IntegrationMutationRequest(@NotNull UUID requestId) {
}
