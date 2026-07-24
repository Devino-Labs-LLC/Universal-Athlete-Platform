package com.devinolabs.uap.identity.infrastructure.web;

public record RegisterResponse(
		String accountId,
		String email,
		String status) {
}
