package com.devinolabs.uap.identity.infrastructure.web;

public record LoginResponse(
		String accountId,
		String status) {
}
