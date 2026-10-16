package com.devinolabs.uap.integrations.application;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.devinolabs.uap.athlete.api.AthleteContextPort;
import com.devinolabs.uap.athlete.api.AthleteRef;
import com.devinolabs.uap.integrations.api.devices.DeviceQualityNoticeView;
import com.devinolabs.uap.integrations.api.devices.DeviceView;
import com.devinolabs.uap.integrations.domain.ActiveConnectionPolicy;
import com.devinolabs.uap.integrations.domain.ConnectedDevice;
import com.devinolabs.uap.integrations.domain.Connection;
import com.devinolabs.uap.integrations.domain.ConnectionId;
import com.devinolabs.uap.integrations.domain.DeviceCapability;
import com.devinolabs.uap.integrations.domain.DeviceId;
import com.devinolabs.uap.integrations.domain.DeviceLifecycle;
import com.devinolabs.uap.integrations.domain.DeviceType;
import com.devinolabs.uap.integrations.domain.EvidenceQualityPolicy;
import com.devinolabs.uap.integrations.domain.SignalFamily;

@Service
public class DeviceService {

	private final DeviceRepository deviceRepository;
	private final ConnectionRepository connectionRepository;
	private final AthleteContextPort athleteContextPort;
	private final Clock clock;

	public DeviceService(
			DeviceRepository deviceRepository,
			ConnectionRepository connectionRepository,
			AthleteContextPort athleteContextPort,
			Clock clock) {
		this.deviceRepository = Objects.requireNonNull(deviceRepository);
		this.connectionRepository = Objects.requireNonNull(connectionRepository);
		this.athleteContextPort = Objects.requireNonNull(athleteContextPort);
		this.clock = Objects.requireNonNull(clock);
	}

	@Transactional(readOnly = true)
	public List<DeviceView> listForAccount(UUID accountId) {
		Instant now = Instant.now(clock);
		return deviceRepository.findByAccountId(accountId).stream()
				.map(device -> toView(device, now))
				.toList();
	}

	@Transactional
	public DeviceView register(
			UUID accountId,
			UUID connectionId,
			String externalDeviceId,
			String deviceType,
			String manufacturer,
			String model,
			String displayName,
			String firmwareVersion,
			List<String> capabilities) {
		Connection connection = requireOwnedConnection(accountId, connectionId, true);
		Set<DeviceCapability> caps = ConnectedDevice.parseCapabilityList(capabilities);
		DeviceType type = ConnectedDevice.parseType(deviceType);
		String name = displayName == null || displayName.isBlank()
				? defaultName(type, connection.provider().name())
				: displayName.trim();
		Optional<ConnectedDevice> existing = deviceRepository.findByAthleteProviderExternal(
				connection.athleteId(),
				connection.provider(),
				externalDeviceId);
		ConnectedDevice saved;
		if (existing.isPresent()) {
			ConnectedDevice device = existing.get();
			if (!device.connectionId().equals(connection.id()) || !device.accountId().equals(accountId)) {
				throw new IntegrationDeviceNotFoundException();
			}
			device.applyDiscovery(type, manufacturer, model, name, firmwareVersion, caps, clock);
			saved = deviceRepository.save(device);
		}
		else {
			saved = deviceRepository.save(ConnectedDevice.register(
					DeviceId.generate(),
					connection.athleteId(),
					accountId,
					connection.id(),
					connection.provider(),
					externalDeviceId,
					type,
					manufacturer,
					model,
					name,
					firmwareVersion,
					caps,
					clock));
		}
		return toView(saved, Instant.now(clock));
	}

	@Transactional
	public ConnectedDevice resolveForIngest(
			Connection connection,
			String externalDeviceId,
			String deviceType,
			String manufacturer,
			String model,
			String displayName,
			String firmwareVersion,
			List<String> capabilities) {
		if (externalDeviceId == null || externalDeviceId.isBlank()) {
			return null;
		}
		DeviceView view = register(
				connection.accountId(),
				connection.id().value(),
				externalDeviceId.trim(),
				deviceType,
				manufacturer,
				model,
				displayName,
				firmwareVersion,
				capabilities);
		ConnectedDevice device = deviceRepository.findById(DeviceId.of(view.deviceId())).orElseThrow();
		device.recordSuccessfulData(clock);
		return deviceRepository.save(device);
	}

	@Transactional
	public DeviceView disconnect(UUID accountId, UUID deviceId) {
		ConnectedDevice device = requireOwnedDevice(accountId, deviceId, true);
		device.disconnect(clock);
		return toView(deviceRepository.save(device), Instant.now(clock));
	}

	@Transactional
	public void disconnectForConnection(Connection connection) {
		for (ConnectedDevice device : deviceRepository.findByConnectionId(connection.id().value())) {
			device.disconnect(clock);
			deviceRepository.save(device);
		}
	}

	@Transactional(readOnly = true)
	public List<DeviceQualityNoticeView> qualityNotices(UUID accountId) {
		Instant now = Instant.now(clock);
		List<DeviceQualityNoticeView> notices = new ArrayList<>();
		for (ConnectedDevice device : deviceRepository.findByAccountId(accountId)) {
			DeviceLifecycle lifecycle = device.effectiveLifecycle(now);
			if (lifecycle == DeviceLifecycle.STALE || device.lastSuccessfulDataAt() == null) {
				if (device.capabilities().contains(DeviceCapability.SLEEP)
						&& (device.lastSuccessfulDataAt() == null
								|| EvidenceQualityPolicy.isStale(SignalFamily.SLEEP, device.lastSuccessfulDataAt(), now))) {
					notices.add(new DeviceQualityNoticeView(
							device.id().value(),
							"SLEEP_STALE",
							"Your sleep data hasn't updated recently."));
				}
				if (device.lastSuccessfulDataAt() == null || lifecycle == DeviceLifecycle.STALE) {
					notices.add(new DeviceQualityNoticeView(
							device.id().value(),
							"DEVICE_STALE",
							device.displayName() + " has not sent data recently."));
				}
			}
			if (!device.capabilities().contains(DeviceCapability.HRV)
					&& (device.deviceType().name().contains("WATCH")
							|| device.capabilities().contains(DeviceCapability.HEART_RATE))) {
				notices.add(new DeviceQualityNoticeView(
						device.id().value(),
						"HRV_UNSUPPORTED",
						"This device does not provide HRV."));
			}
		}
		return List.copyOf(notices);
	}

	public DeviceView toView(ConnectedDevice device, Instant now) {
		return new DeviceView(
				device.id().value(),
				device.connectionId().value(),
				device.provider().name(),
				device.externalDeviceId(),
				device.deviceType().name(),
				device.manufacturer(),
				device.model(),
				device.displayName(),
				device.firmwareVersion(),
				device.capabilities().stream().map(Enum::name).sorted().toList(),
				device.effectiveLifecycle(now).name(),
				device.firstSeenAt(),
				device.lastSeenAt(),
				device.lastSuccessfulDataAt());
	}

	private ConnectedDevice requireOwnedDevice(UUID accountId, UUID deviceId, boolean forUpdate) {
		AthleteRef athlete = forUpdate
				? athleteContextPort.requireMutableAthleteForUpdate(accountId)
				: athleteContextPort.requireAthlete(accountId);
		ConnectedDevice device = deviceRepository.findById(DeviceId.of(deviceId))
				.orElseThrow(IntegrationDeviceNotFoundException::new);
		if (!device.accountId().equals(accountId) || !device.athleteId().equals(athlete.athleteId())) {
			throw new IntegrationDeviceNotFoundException();
		}
		return device;
	}

	private Connection requireOwnedConnection(UUID accountId, UUID connectionId, boolean forUpdate) {
		AthleteRef athlete = forUpdate
				? athleteContextPort.requireMutableAthleteForUpdate(accountId)
				: athleteContextPort.requireAthlete(accountId);
		Connection connection = connectionRepository.findById(ConnectionId.of(connectionId))
				.orElseThrow(IntegrationConnectionNotFoundException::new);
		if (!ActiveConnectionPolicy.owns(connection, accountId, athlete.athleteId())) {
			throw new IntegrationConnectionNotFoundException();
		}
		return connection;
	}

	private static String defaultName(DeviceType type, String provider) {
		return type.name().charAt(0) + type.name().substring(1).toLowerCase(Locale.ROOT).replace('_', ' ')
				+ " (" + provider + ")";
	}
}
