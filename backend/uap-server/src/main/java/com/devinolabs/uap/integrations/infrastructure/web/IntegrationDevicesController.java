package com.devinolabs.uap.integrations.infrastructure.web;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.integrations.api.devices.DeviceQualityNoticeView;
import com.devinolabs.uap.integrations.api.devices.DeviceView;
import com.devinolabs.uap.integrations.api.evidence.FusedEvidenceView;
import com.devinolabs.uap.integrations.api.evidence.WearableOpsSummaryView;
import com.devinolabs.uap.integrations.application.DeviceService;
import com.devinolabs.uap.integrations.application.SensorFusionService;
import com.devinolabs.uap.integrations.application.WearableOpsService;
import com.devinolabs.uap.athlete.api.AthleteContextPort;

@RestController
@RequestMapping("/api/v1/integrations")
@ConditionalOnProperty(prefix = "uap.integrations", name = "enabled", havingValue = "true", matchIfMissing = true)
class IntegrationDevicesController {

	private final DeviceService deviceService;
	private final SensorFusionService fusionService;
	private final WearableOpsService opsService;
	private final AthleteContextPort athleteContextPort;

	IntegrationDevicesController(
			DeviceService deviceService,
			SensorFusionService fusionService,
			WearableOpsService opsService,
			AthleteContextPort athleteContextPort) {
		this.deviceService = deviceService;
		this.fusionService = fusionService;
		this.opsService = opsService;
		this.athleteContextPort = athleteContextPort;
	}

	@GetMapping("/devices")
	List<DeviceView> list(Authentication authentication) {
		return deviceService.listForAccount(accountId(authentication));
	}

	@PostMapping("/devices")
	@ResponseStatus(HttpStatus.CREATED)
	DeviceView register(@Valid @RequestBody DeviceWriteRequest request, Authentication authentication) {
		return deviceService.register(
				accountId(authentication),
				request.connectionId(),
				request.externalDeviceId(),
				request.deviceType(),
				request.manufacturer(),
				request.model(),
				request.displayName(),
				request.firmwareVersion(),
				request.capabilities());
	}

	@PostMapping("/devices/{deviceId}/disconnect")
	DeviceView disconnect(@PathVariable UUID deviceId, Authentication authentication) {
		return deviceService.disconnect(accountId(authentication), deviceId);
	}

	@GetMapping("/devices/quality-feedback")
	List<DeviceQualityNoticeView> quality(Authentication authentication) {
		return deviceService.qualityNotices(accountId(authentication));
	}

	@GetMapping("/fused-evidence")
	List<FusedEvidenceView> fused(
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(value = "family", required = false) String family,
			Authentication authentication) {
		UUID athleteId = athleteContextPort.requireAthlete(accountId(authentication)).athleteId();
		return fusionService.listFused(athleteId, from, to, family);
	}

	@GetMapping("/ops/summary")
	WearableOpsSummaryView ops(Authentication authentication) {
		return opsService.summaryForAccount(accountId(authentication));
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}
}
