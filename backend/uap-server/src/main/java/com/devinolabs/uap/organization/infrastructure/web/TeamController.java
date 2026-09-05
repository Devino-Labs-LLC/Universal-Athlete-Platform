package com.devinolabs.uap.organization.infrastructure.web;

import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.identity.infrastructure.security.AccountPrincipal;
import com.devinolabs.uap.organization.application.ArchiveTeamUseCase;
import com.devinolabs.uap.organization.application.GetTeamUseCase;
import com.devinolabs.uap.organization.application.UpdateTeamUseCase;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.TeamId;

@RestController
@RequestMapping("/api/v1/teams")
class TeamController {

	private final GetTeamUseCase getTeamUseCase;
	private final UpdateTeamUseCase updateTeamUseCase;
	private final ArchiveTeamUseCase archiveTeamUseCase;

	TeamController(
			GetTeamUseCase getTeamUseCase,
			UpdateTeamUseCase updateTeamUseCase,
			ArchiveTeamUseCase archiveTeamUseCase) {
		this.getTeamUseCase = Objects.requireNonNull(getTeamUseCase);
		this.updateTeamUseCase = Objects.requireNonNull(updateTeamUseCase);
		this.archiveTeamUseCase = Objects.requireNonNull(archiveTeamUseCase);
	}

	@GetMapping("/{teamId}")
	TeamResponse get(@PathVariable UUID teamId, Authentication authentication) {
		return TeamResponse.from(getTeamUseCase.execute(accountId(authentication), TeamId.of(teamId)));
	}

	@PatchMapping("/{teamId}")
	TeamResponse update(
			@PathVariable UUID teamId,
			@Valid @RequestBody UpdateTeamRequest request,
			Authentication authentication) {
		return TeamResponse.from(updateTeamUseCase.execute(
				accountId(authentication),
				TeamId.of(teamId),
				request.name(),
				request.expectedVersion()));
	}

	@PostMapping("/{teamId}/archive")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(@PathVariable UUID teamId, Authentication authentication) {
		archiveTeamUseCase.execute(accountId(authentication), TeamId.of(teamId));
	}

	private static AccountId accountId(Authentication authentication) {
		AccountPrincipal principal = requirePrincipal(authentication);
		return AccountId.of(principal.accountUuid());
	}

	private static AccountPrincipal requirePrincipal(Authentication authentication) {
		if (authentication == null || !(authentication.getPrincipal() instanceof AccountPrincipal principal)) {
			throw new IllegalStateException("Authenticated AccountPrincipal is required");
		}
		return principal;
	}

}
