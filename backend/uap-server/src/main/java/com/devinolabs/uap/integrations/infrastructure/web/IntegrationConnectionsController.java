package com.devinolabs.uap.integrations.infrastructure.web;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.integrations.api.connections.ConnectionView;
import com.devinolabs.uap.integrations.api.connections.SyncRunView;
import com.devinolabs.uap.integrations.application.IntegrationConnectionService;

@RestController
@RequestMapping("/api/v1/integrations/connections")
@ConditionalOnProperty(prefix = "uap.integrations", name = "enabled", havingValue = "true", matchIfMissing = true)
class IntegrationConnectionsController {

	private final IntegrationConnectionService connectionService;

	IntegrationConnectionsController(IntegrationConnectionService connectionService) {
		this.connectionService = connectionService;
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

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal.accountUuid();
	}

}
