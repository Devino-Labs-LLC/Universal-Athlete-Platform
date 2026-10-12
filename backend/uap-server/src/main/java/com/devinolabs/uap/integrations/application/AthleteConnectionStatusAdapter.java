package com.devinolabs.uap.integrations.application;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.devinolabs.uap.integrations.api.connections.AthleteConnectionStatusPort;
import com.devinolabs.uap.integrations.api.connections.ConnectionView;

@Service
class AthleteConnectionStatusAdapter implements AthleteConnectionStatusPort {

	private final IntegrationConnectionService connectionService;

	AthleteConnectionStatusAdapter(IntegrationConnectionService connectionService) {
		this.connectionService = Objects.requireNonNull(connectionService);
	}

	@Override
	public List<ConnectionView> listForAccount(UUID accountId) {
		return connectionService.listConnectionsForAccount(accountId);
	}

}
