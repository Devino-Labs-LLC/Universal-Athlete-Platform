package com.devinolabs.uap.sponsorship.application;

import java.util.UUID;

import org.springframework.stereotype.Component;

import com.devinolabs.uap.sponsorship.api.CatalogAccessDeniedException;
import com.devinolabs.uap.sponsorship.api.SponsorOperatorPort;
import com.devinolabs.uap.sponsorship.infrastructure.config.SponsorshipProperties;

@Component
public class SponsorOperatorGuard implements SponsorOperatorPort {

	private final SponsorshipProperties properties;

	public SponsorOperatorGuard(SponsorshipProperties properties) {
		this.properties = properties;
	}

	public void requireOperator(UUID accountId) {
		if (!properties.isOperator(accountId)) {
			throw new CatalogAccessDeniedException();
		}
	}

	public boolean isOperator(UUID accountId) {
		return properties.isOperator(accountId);
	}
}
