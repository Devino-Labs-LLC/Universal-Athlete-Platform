package com.devinolabs.uap.integrations.domain;

import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class ConnectedDevice {

	private final DeviceId id;
	private final UUID athleteId;
	private final UUID accountId;
	private final ConnectionId connectionId;
	private final HealthProviderKey provider;
	private final String externalDeviceId;
	private DeviceType deviceType;
	private String manufacturer;
	private String model;
	private String displayName;
	private String firmwareVersion;
	private final EnumSet<DeviceCapability> capabilities;
	private DeviceLifecycle lifecycleState;
	private final Instant firstSeenAt;
	private Instant lastSeenAt;
	private Instant lastSuccessfulDataAt;
	private Instant updatedAt;
	private long version;

	private ConnectedDevice(
			DeviceId id,
			UUID athleteId,
			UUID accountId,
			ConnectionId connectionId,
			HealthProviderKey provider,
			String externalDeviceId,
			DeviceType deviceType,
			String manufacturer,
			String model,
			String displayName,
			String firmwareVersion,
			Set<DeviceCapability> capabilities,
			DeviceLifecycle lifecycleState,
			Instant firstSeenAt,
			Instant lastSeenAt,
			Instant lastSuccessfulDataAt,
			Instant updatedAt,
			long version) {
		this.id = Objects.requireNonNull(id, "id must not be null");
		this.athleteId = Objects.requireNonNull(athleteId, "athleteId must not be null");
		this.accountId = Objects.requireNonNull(accountId, "accountId must not be null");
		this.connectionId = Objects.requireNonNull(connectionId, "connectionId must not be null");
		this.provider = Objects.requireNonNull(provider, "provider must not be null");
		this.externalDeviceId = requireNonBlank(externalDeviceId, "externalDeviceId");
		if (this.externalDeviceId.length() > 191) {
			throw new IllegalArgumentException("externalDeviceId must be <= 191 characters");
		}
		this.deviceType = Objects.requireNonNull(deviceType, "deviceType must not be null");
		this.manufacturer = normalizeNullable(manufacturer);
		this.model = normalizeNullable(model);
		this.displayName = requireNonBlank(displayName, "displayName");
		this.firmwareVersion = normalizeNullable(firmwareVersion);
		this.capabilities = EnumSet.noneOf(DeviceCapability.class);
		if (capabilities != null) {
			this.capabilities.addAll(capabilities);
		}
		this.lifecycleState = Objects.requireNonNull(lifecycleState, "lifecycleState must not be null");
		this.firstSeenAt = Objects.requireNonNull(firstSeenAt, "firstSeenAt must not be null");
		this.lastSeenAt = Objects.requireNonNull(lastSeenAt, "lastSeenAt must not be null");
		this.lastSuccessfulDataAt = lastSuccessfulDataAt;
		this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
		if (version < 0) {
			throw new IllegalArgumentException("Version must not be negative");
		}
		this.version = version;
	}

	public static ConnectedDevice register(
			DeviceId id,
			UUID athleteId,
			UUID accountId,
			ConnectionId connectionId,
			HealthProviderKey provider,
			String externalDeviceId,
			DeviceType deviceType,
			String manufacturer,
			String model,
			String displayName,
			String firmwareVersion,
			Set<DeviceCapability> capabilities,
			Clock clock) {
		Instant now = Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
		return new ConnectedDevice(
				id,
				athleteId,
				accountId,
				connectionId,
				provider,
				externalDeviceId,
				deviceType,
				manufacturer,
				model,
				displayName,
				firmwareVersion,
				capabilities,
				DeviceLifecycle.ACTIVE,
				now,
				now,
				null,
				now,
				0L);
	}

	public static ConnectedDevice rehydrate(
			DeviceId id,
			UUID athleteId,
			UUID accountId,
			ConnectionId connectionId,
			HealthProviderKey provider,
			String externalDeviceId,
			DeviceType deviceType,
			String manufacturer,
			String model,
			String displayName,
			String firmwareVersion,
			Set<DeviceCapability> capabilities,
			DeviceLifecycle lifecycleState,
			Instant firstSeenAt,
			Instant lastSeenAt,
			Instant lastSuccessfulDataAt,
			Instant updatedAt,
			long version) {
		return new ConnectedDevice(
				id,
				athleteId,
				accountId,
				connectionId,
				provider,
				externalDeviceId,
				deviceType,
				manufacturer,
				model,
				displayName,
				firmwareVersion,
				capabilities,
				lifecycleState,
				firstSeenAt,
				lastSeenAt,
				lastSuccessfulDataAt,
				updatedAt,
				version);
	}

	public void applyDiscovery(
			DeviceType deviceType,
			String manufacturer,
			String model,
			String displayName,
			String firmwareVersion,
			Set<DeviceCapability> incomingCapabilities,
			Clock clock) {
		Instant now = Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
		this.deviceType = Objects.requireNonNull(deviceType, "deviceType must not be null");
		this.manufacturer = normalizeNullable(manufacturer);
		this.model = normalizeNullable(model);
		this.displayName = requireNonBlank(displayName, "displayName");
		this.firmwareVersion = normalizeNullable(firmwareVersion);
		if (incomingCapabilities != null && !incomingCapabilities.isEmpty()) {
			this.capabilities.clear();
			this.capabilities.addAll(incomingCapabilities);
		}
		if (this.lifecycleState == DeviceLifecycle.DISCONNECTED) {
			this.lifecycleState = DeviceLifecycle.ACTIVE;
		}
		this.lastSeenAt = now;
		this.updatedAt = now;
	}

	public void recordSuccessfulData(Clock clock) {
		Instant now = Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
		this.lastSuccessfulDataAt = now;
		this.lastSeenAt = now;
		if (this.lifecycleState != DeviceLifecycle.DISCONNECTED) {
			this.lifecycleState = DeviceLifecycle.ACTIVE;
		}
		this.updatedAt = now;
	}

	public void disconnect(Clock clock) {
		Instant now = Instant.now(Objects.requireNonNull(clock, "clock must not be null"));
		this.lifecycleState = DeviceLifecycle.DISCONNECTED;
		this.updatedAt = now;
	}

	public DeviceLifecycle effectiveLifecycle(Instant now) {
		if (lifecycleState == DeviceLifecycle.DISCONNECTED) {
			return DeviceLifecycle.DISCONNECTED;
		}
		Instant anchor = lastSuccessfulDataAt != null ? lastSuccessfulDataAt : lastSeenAt;
		if (anchor != null && now != null && anchor.isBefore(now.minus(java.time.Duration.ofHours(48)))) {
			return DeviceLifecycle.STALE;
		}
		return DeviceLifecycle.ACTIVE;
	}

	public String capabilitiesCsv() {
		return capabilities.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
	}

	public static Set<DeviceCapability> parseCapabilitiesCsv(String csv) {
		if (csv == null || csv.isBlank()) {
			return Set.of();
		}
		Set<DeviceCapability> parsed = new LinkedHashSet<>();
		for (String token : csv.split(",")) {
			DeviceCapability.tryParse(token).ifPresent(parsed::add);
		}
		return parsed;
	}

	public static Set<DeviceCapability> parseCapabilityList(java.util.List<String> raw) {
		if (raw == null || raw.isEmpty()) {
			return Set.of();
		}
		Set<DeviceCapability> parsed = new LinkedHashSet<>();
		for (String token : raw) {
			DeviceCapability capability = DeviceCapability.tryParse(token)
					.orElseThrow(() -> new IllegalArgumentException("Unknown device capability: " + token));
			parsed.add(capability);
		}
		return parsed;
	}

	public static DeviceType parseType(String raw) {
		if (raw == null || raw.isBlank()) {
			return DeviceType.OTHER;
		}
		try {
			return DeviceType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
		}
		catch (IllegalArgumentException ex) {
			throw new IllegalArgumentException("Unknown device type: " + raw);
		}
	}

	private static String requireNonBlank(String value, String field) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(field + " is required");
		}
		return value.trim();
	}

	private static String normalizeNullable(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	public DeviceId id() {
		return id;
	}

	public UUID athleteId() {
		return athleteId;
	}

	public UUID accountId() {
		return accountId;
	}

	public ConnectionId connectionId() {
		return connectionId;
	}

	public HealthProviderKey provider() {
		return provider;
	}

	public String externalDeviceId() {
		return externalDeviceId;
	}

	public DeviceType deviceType() {
		return deviceType;
	}

	public String manufacturer() {
		return manufacturer;
	}

	public String model() {
		return model;
	}

	public String displayName() {
		return displayName;
	}

	public String firmwareVersion() {
		return firmwareVersion;
	}

	public Set<DeviceCapability> capabilities() {
		return Collections.unmodifiableSet(capabilities);
	}

	public DeviceLifecycle lifecycleState() {
		return lifecycleState;
	}

	public Instant firstSeenAt() {
		return firstSeenAt;
	}

	public Instant lastSeenAt() {
		return lastSeenAt;
	}

	public Instant lastSuccessfulDataAt() {
		return lastSuccessfulDataAt;
	}

	public Instant updatedAt() {
		return updatedAt;
	}

	public Instant createdAt() {
		return firstSeenAt;
	}

	public long version() {
		return version;
	}
}
