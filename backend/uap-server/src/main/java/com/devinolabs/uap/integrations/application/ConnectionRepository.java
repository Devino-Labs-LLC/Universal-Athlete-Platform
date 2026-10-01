package com.devinolabs.uap.integrations.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;

public interface ConnectionRepository {

	Connection save(Connection connection);

	Optional<Connection> findById(ConnectionId id);

	List<Connection> findByAccountId(UUID accountId);

	List<Connection> findByAthleteId(UUID athleteId);

	Optional<Connection> findByAthleteIdAndProvider(UUID athleteId, HealthProviderKey provider);

	long countByAccountId(UUID accountId);

}
