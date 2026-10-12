package com.devinolabs.uap.sponsorship.infrastructure.web;

public record SponsorWriteRequest(
		String name,
		String displayName,
		String logoUrl,
		String websiteUrl,
		String description) {
}
