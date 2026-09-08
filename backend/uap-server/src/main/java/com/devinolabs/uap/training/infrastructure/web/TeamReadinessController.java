package com.devinolabs.uap.training.infrastructure.web;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.training.application.GetTeamReadinessUseCase;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/readiness")
class TeamReadinessController {

	private final GetTeamReadinessUseCase getTeamReadinessUseCase;

	TeamReadinessController(GetTeamReadinessUseCase getTeamReadinessUseCase) {
		this.getTeamReadinessUseCase = Objects.requireNonNull(getTeamReadinessUseCase);
	}

	@GetMapping
	TeamReadinessResponse readiness(
			@PathVariable UUID teamId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
			Authentication authentication) {
		return TeamReadinessResponse.from(
				getTeamReadinessUseCase.execute(accountId(authentication), teamId, date));
	}

	private static UUID accountId(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated account is required");
		}
		return principal.accountUuid();
	}

}
