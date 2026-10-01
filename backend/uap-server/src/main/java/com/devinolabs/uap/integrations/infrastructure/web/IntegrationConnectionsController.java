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
import com.devinolabs.uap.integrations.api.connections.ConnectionView;
import com.devinolabs.uap.integrations.api.connections.EvidenceBatchResultView;
import com.devinolabs.uap.integrations.api.connections.SyncRunView;
import com.devinolabs.uap.integrations.api.evidence.ConnectedEvidenceView;
import com.devinolabs.uap.integrations.application.IntegrationConnectionService;
import com.devinolabs.uap.integrations.application.IntegrationEvidenceService;
import com.devinolabs.uap.integrations.application.IntegrationEvidenceService.EvidenceBatchItemCommand;
import com.devinolabs.uap.integrations.application.IntegrationValidationException;

@RestController
@RequestMapping("/api/v1/integrations/connections")
@ConditionalOnProperty(prefix = "uap.integrations", name = "enabled", havingValue = "true", matchIfMissing = true)
class IntegrationConnectionsController {

	private final IntegrationConnectionService connectionService;
	private final IntegrationEvidenceService evidenceService;

	IntegrationConnectionsController(
			IntegrationConnectionService connectionService,
			IntegrationEvidenceService evidenceService) {
		this.connectionService = connectionService;
		this.evidenceService = evidenceService;
	}

	@GetMapping
	List<ConnectionView> list(Authentication authentication) {
		return connectionService.listConnectionsForAccount(accountId(authentication));
	}

	@GetMapping("/{connectionId}")
	ConnectionView get(@PathVariable UUID connectionId, Authentication authentication) {
		return connectionService.getConnection(accountId(authentication), connectionId);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	ConnectionView beginConnect(
			@Valid @RequestBody BeginConnectRequest request,
			Authentication authentication) {
		return connectionService.beginConnect(
				accountId(authentication),
				request.requestId(),
				request.provider());
	}

	@PostMapping("/{connectionId}/confirm")
	ConnectionView confirm(@PathVariable UUID connectionId, Authentication authentication) {
		return connectionService.confirmConnected(accountId(authentication), connectionId);
	}

	@PostMapping("/{connectionId}/disconnect")
	ConnectionView disconnect(
			@PathVariable UUID connectionId,
			@Valid @RequestBody IntegrationMutationRequest request,
			Authentication authentication) {
		return connectionService.disconnect(
				accountId(authentication),
				connectionId,
				request.requestId());
	}

	@PostMapping("/{connectionId}/sync")
	SyncRunView sync(
			@PathVariable UUID connectionId,
			@Valid @RequestBody IntegrationMutationRequest request,
			Authentication authentication) {
		return connectionService.requestSync(
				accountId(authentication),
				connectionId,
				request.requestId());
	}

	@GetMapping("/{connectionId}/sync-runs")
	List<SyncRunView> listSyncRuns(@PathVariable UUID connectionId, Authentication authentication) {
		return connectionService.listSyncRuns(accountId(authentication), connectionId);
	}

	@PostMapping("/{connectionId}/evidence-batches")
	@ResponseStatus(HttpStatus.ACCEPTED)
	EvidenceBatchResultView uploadEvidenceBatch(
			@PathVariable UUID connectionId,
			@Valid @RequestBody EvidenceBatchRequest request,
			Authentication authentication) {
		List<EvidenceBatchItemCommand> items = request.items().stream()
				.map(item -> new EvidenceBatchItemCommand(
						item.externalRecordId(),
						item.signalFamily(),
						item.signalType(),
						item.valueNumeric(),
						item.valueText(),
						item.unitCode(),
						item.periodStart(),
						item.periodEnd(),
						item.observedAt(),
						item.providerUpdatedAt(),
						item.sourceDeviceOrApp(),
						item.provenanceClass()))
				.toList();
		return evidenceService.uploadEvidenceBatch(
				accountId(authentication),
				connectionId,
				request.requestId(),
				items);
	}

	/**
	 * Read-only evidence listing. Never syncs or ingests (V1 no-hidden-write).
	 */
	@GetMapping("/{connectionId}/evidence")
	List<ConnectedEvidenceView> listEvidence(
			@PathVariable UUID connectionId,
			@RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
			@RequestParam(value = "family", required = false) String family,
			Authentication authentication) {
		if (from == null || to == null) {
			throw new IntegrationValidationException("VALIDATION_ERROR", "from and to are required");
		}
		return evidenceService.listEvidenceForConnection(
				accountId(authentication),
				connectionId,
				from,
				to,
				family);
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}

}
