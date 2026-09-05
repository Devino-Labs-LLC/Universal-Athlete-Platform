package com.devinolabs.uap.organization.infrastructure.web;

import java.util.List;
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
import com.devinolabs.uap.organization.application.ArchiveOrganizationUseCase;
import com.devinolabs.uap.organization.application.CreateOrganizationUseCase;
import com.devinolabs.uap.organization.application.CreateTeamUseCase;
import com.devinolabs.uap.organization.application.GetOrganizationUseCase;
import com.devinolabs.uap.organization.application.ListOrganizationsForAccountUseCase;
import com.devinolabs.uap.organization.application.ListTeamsForOrganizationUseCase;
import com.devinolabs.uap.organization.application.UpdateOrganizationUseCase;
import com.devinolabs.uap.organization.domain.AccountId;
import com.devinolabs.uap.organization.domain.OrganizationId;

@RestController
@RequestMapping("/api/v1/organizations")
class OrganizationController {

	private final CreateOrganizationUseCase createOrganizationUseCase;
	private final GetOrganizationUseCase getOrganizationUseCase;
	private final ListOrganizationsForAccountUseCase listOrganizationsForAccountUseCase;
	private final UpdateOrganizationUseCase updateOrganizationUseCase;
	private final ArchiveOrganizationUseCase archiveOrganizationUseCase;
	private final CreateTeamUseCase createTeamUseCase;
	private final ListTeamsForOrganizationUseCase listTeamsForOrganizationUseCase;

	OrganizationController(
			CreateOrganizationUseCase createOrganizationUseCase,
			GetOrganizationUseCase getOrganizationUseCase,
			ListOrganizationsForAccountUseCase listOrganizationsForAccountUseCase,
			UpdateOrganizationUseCase updateOrganizationUseCase,
			ArchiveOrganizationUseCase archiveOrganizationUseCase,
			CreateTeamUseCase createTeamUseCase,
			ListTeamsForOrganizationUseCase listTeamsForOrganizationUseCase) {
		this.createOrganizationUseCase = Objects.requireNonNull(createOrganizationUseCase);
		this.getOrganizationUseCase = Objects.requireNonNull(getOrganizationUseCase);
		this.listOrganizationsForAccountUseCase = Objects.requireNonNull(listOrganizationsForAccountUseCase);
		this.updateOrganizationUseCase = Objects.requireNonNull(updateOrganizationUseCase);
		this.archiveOrganizationUseCase = Objects.requireNonNull(archiveOrganizationUseCase);
		this.createTeamUseCase = Objects.requireNonNull(createTeamUseCase);
		this.listTeamsForOrganizationUseCase = Objects.requireNonNull(listTeamsForOrganizationUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request, Authentication authentication) {
		return OrganizationResponse.from(
				createOrganizationUseCase.execute(accountId(authentication), request.name()));
	}

	@GetMapping
	List<OrganizationResponse> list(Authentication authentication) {
		return listOrganizationsForAccountUseCase.execute(accountId(authentication)).stream()
				.map(OrganizationResponse::from)
				.toList();
	}

	@GetMapping("/{organizationId}")
	OrganizationResponse get(@PathVariable UUID organizationId, Authentication authentication) {
		return OrganizationResponse.from(
				getOrganizationUseCase.execute(accountId(authentication), OrganizationId.of(organizationId)));
	}

	@PatchMapping("/{organizationId}")
	OrganizationResponse update(
			@PathVariable UUID organizationId,
			@Valid @RequestBody UpdateOrganizationRequest request,
			Authentication authentication) {
		return OrganizationResponse.from(updateOrganizationUseCase.execute(
				accountId(authentication),
				OrganizationId.of(organizationId),
				request.name(),
				request.expectedVersion()));
	}

	@PostMapping("/{organizationId}/archive")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(@PathVariable UUID organizationId, Authentication authentication) {
		archiveOrganizationUseCase.execute(accountId(authentication), OrganizationId.of(organizationId));
	}

	@PostMapping("/{organizationId}/teams")
	@ResponseStatus(HttpStatus.CREATED)
	TeamResponse createTeam(
			@PathVariable UUID organizationId,
			@Valid @RequestBody CreateTeamRequest request,
			Authentication authentication) {
		return TeamResponse.from(createTeamUseCase.execute(
				accountId(authentication),
				OrganizationId.of(organizationId),
				request.name()));
	}

	@GetMapping("/{organizationId}/teams")
	List<TeamResponse> listTeams(@PathVariable UUID organizationId, Authentication authentication) {
		return listTeamsForOrganizationUseCase.execute(accountId(authentication), OrganizationId.of(organizationId))
				.stream()
				.map(TeamResponse::from)
				.toList();
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
