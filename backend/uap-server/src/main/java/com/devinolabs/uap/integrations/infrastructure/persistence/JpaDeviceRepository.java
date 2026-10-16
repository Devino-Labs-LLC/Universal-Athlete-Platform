package com.devinolabs.uap.integrations.infrastructure.persistence;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.devinolabs.uap.integrations.application.DeviceRepository;
import com.devinolabs.uap.integrations.domain.ConnectedDevice;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.DeviceId;
import com.devinolabs.uap.integrations.domain.HealthProviderKey;

@Repository
class JpaDeviceRepository implements DeviceRepository {

	private final IntegrationDeviceJpaRepository jpaRepository;

	JpaDeviceRepository(IntegrationDeviceJpaRepository jpaRepository) {
		this.jpaRepository = Objects.requireNonNull(jpaRepository);
	}

	@Override
	public ConnectedDevice save(ConnectedDevice device) {
		Optional<IntegrationDeviceJpaEntity> existing = jpaRepository.findById(device.id().value());
		IntegrationDeviceJpaEntity saved;
		if (existing.isEmpty()) {
			saved = jpaRepository.save(toEntity(device, true));
		}
		else {
			IntegrationDeviceJpaEntity entity = existing.get();
			entity.applyDomainState(
					device.deviceType(),
					device.manufacturer(),
					device.model(),
					device.displayName(),
					device.firmwareVersion(),
					device.capabilitiesCsv(),
					device.lifecycleState(),
					device.lastSeenAt(),
					device.lastSuccessfulDataAt(),
					device.updatedAt());
			saved = jpaRepository.save(entity);
		}
		jpaRepository.flush();
		return toDomain(saved);
	}

	@Override
	public Optional<ConnectedDevice> findById(DeviceId id) {
		return jpaRepository.findById(id.value()).map(JpaDeviceRepository::toDomain);
	}

	@Override
	public List<ConnectedDevice> findByAccountId(UUID accountId) {
		return jpaRepository.findByAccountIdOrderByLastSeenAtDesc(accountId).stream()
				.map(JpaDeviceRepository::toDomain)
				.toList();
	}

	@Override
	public List<ConnectedDevice> findByAthleteId(UUID athleteId) {
		return jpaRepository.findByAthleteIdOrderByLastSeenAtDesc(athleteId).stream()
				.map(JpaDeviceRepository::toDomain)
				.toList();
	}

	@Override
	public List<ConnectedDevice> findByConnectionId(UUID connectionId) {
		return jpaRepository.findByConnectionId(connectionId).stream()
				.map(JpaDeviceRepository::toDomain)
				.toList();
	}

	@Override
	public Optional<ConnectedDevice> findByAthleteProviderExternal(
			UUID athleteId,
			HealthProviderKey provider,
			String externalDeviceId) {
		return jpaRepository
				.findByAthleteIdAndProviderAndExternalDeviceId(athleteId, provider, externalDeviceId)
				.map(JpaDeviceRepository::toDomain);
	}

	private static IntegrationDeviceJpaEntity toEntity(ConnectedDevice device, boolean isNew) {
		return new IntegrationDeviceJpaEntity(
				device.id().value(),
				device.athleteId(),
				device.accountId(),
				device.connectionId().value(),
				device.provider(),
				device.externalDeviceId(),
				device.deviceType(),
				device.manufacturer(),
				device.model(),
				device.displayName(),
				device.firmwareVersion(),
				device.capabilitiesCsv(),
				device.lifecycleState(),
				device.firstSeenAt(),
				device.lastSeenAt(),
				device.lastSuccessfulDataAt(),
				device.createdAt(),
				device.updatedAt(),
				device.version(),
				isNew);
	}

	private static ConnectedDevice toDomain(IntegrationDeviceJpaEntity entity) {
		return ConnectedDevice.rehydrate(
				DeviceId.of(entity.getId()),
				entity.getAthleteId(),
				entity.getAccountId(),
				ConnectionId.of(entity.getConnectionId()),
				entity.getProvider(),
				entity.getExternalDeviceId(),
				entity.getDeviceType(),
				entity.getManufacturer(),
				entity.getModel(),
				entity.getDisplayName(),
				entity.getFirmwareVersion(),
				ConnectedDevice.parseCapabilitiesCsv(entity.getCapabilities()),
				entity.getLifecycleState(),
				entity.getFirstSeenAt(),
				entity.getLastSeenAt(),
				entity.getLastSuccessfulDataAt(),
				entity.getUpdatedAt(),
				entity.getVersion());
	}
}
