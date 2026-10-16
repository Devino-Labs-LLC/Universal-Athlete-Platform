package com.devinolabs.uap.integrations.application;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.devinolabs.uap.integrations.domain.ConnectedDevice;
import com.devinolabs.uap.integrations.domain.DeviceId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;

public interface DeviceRepository {

	ConnectedDevice save(ConnectedDevice device);

	Optional<ConnectedDevice> findById(DeviceId id);

	List<ConnectedDevice> findByAccountId(UUID accountId);

	List<ConnectedDevice> findByAthleteId(UUID athleteId);

	List<ConnectedDevice> findByConnectionId(UUID connectionId);

	Optional<ConnectedDevice> findByAthleteProviderExternal(
			UUID athleteId,
			HealthProviderKey provider,
			String externalDeviceId);
}
