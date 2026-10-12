package com.devinolabs.uap.sponsorship.infrastructure.config;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "uap.sponsorship")
public class SponsorshipProperties {

	private final List<UUID> operatorAccountIds = new ArrayList<>();

	public List<UUID> getOperatorAccountIds() {
		return operatorAccountIds;
	}

	public void setOperatorAccountIds(String operatorAccountIds) {
		this.operatorAccountIds.clear();
		if (operatorAccountIds == null || operatorAccountIds.isBlank()) {
			return;
		}
		for (String part : operatorAccountIds.split(",")) {
			if (part.isBlank()) {
				continue;
			}
			this.operatorAccountIds.add(UUID.fromString(part.trim()));
		}
	}

	public boolean isOperator(UUID accountId) {
		return accountId != null && operatorAccountIds.contains(accountId);
	}
}
