package com.devinolabs.uap.organization.infrastructure.web;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.devinolabs.uap.organization.application.ArchiveOrganizationUseCase;
import com.devinolabs.uap.organization.application.CreateOrganizationInvitationUseCase;
import com.devinolabs.uap.organization.application.CreateOrganizationUseCase;
import com.devinolabs.uap.organization.application.CreateTeamUseCase;
import com.devinolabs.uap.organization.application.GetOrganizationUseCase;
import com.devinolabs.uap.organization.application.LeaveOrganizationUseCase;
import com.devinolabs.uap.organization.application.ListOrganizationInvitationsUseCase;
import com.devinolabs.uap.organization.application.ListOrganizationMembershipsUseCase;
import com.devinolabs.uap.organization.application.ListOrganizationsForAccountUseCase;
import com.devinolabs.uap.organization.application.ListTeamsForOrganizationUseCase;
import com.devinolabs.uap.organization.application.RemoveOrganizationMemberUseCase;
import com.devinolabs.uap.organization.application.RevokeInvitationUseCase;
import com.devinolabs.uap.organization.application.UpdateOrganizationUseCase;
import com.devinolabs.uap.organization.domain.InvitationId;
import com.devinolabs.uap.organization.domain.OrganizationId;
import com.devinolabs.uap.organization.domain.OrganizationMembershipId;

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
	private final CreateOrganizationInvitationUseCase createOrganizationInvitationUseCase;
	private final ListOrganizationInvitationsUseCase listOrganizationInvitationsUseCase;
	private final RevokeInvitationUseCase revokeInvitationUseCase;
	private final ListOrganizationMembershipsUseCase listOrganizationMembershipsUseCase;
	private final RemoveOrganizationMemberUseCase removeOrganizationMemberUseCase;
	private final LeaveOrganizationUseCase leaveOrganizationUseCase;

	OrganizationController(
			CreateOrganizationUseCase createOrganizationUseCase,
			GetOrganizationUseCase getOrganizationUseCase,
			ListOrganizationsForAccountUseCase listOrganizationsForAccountUseCase,
			UpdateOrganizationUseCase updateOrganizationUseCase,
			ArchiveOrganizationUseCase archiveOrganizationUseCase,
			CreateTeamUseCase createTeamUseCase,
			ListTeamsForOrganizationUseCase listTeamsForOrganizationUseCase,
			CreateOrganizationInvitationUseCase createOrganizationInvitationUseCase,
			ListOrganizationInvitationsUseCase listOrganizationInvitationsUseCase,
			RevokeInvitationUseCase revokeInvitationUseCase,
			ListOrganizationMembershipsUseCase listOrganizationMembershipsUseCase,
			RemoveOrganizationMemberUseCase removeOrganizationMemberUseCase,
			LeaveOrganizationUseCase leaveOrganizationUseCase) {
		this.createOrganizationUseCase = Objects.requireNonNull(createOrganizationUseCase);
		this.getOrganizationUseCase = Objects.requireNonNull(getOrganizationUseCase);
		this.listOrganizationsForAccountUseCase = Objects.requireNonNull(listOrganizationsForAccountUseCase);
		this.updateOrganizationUseCase = Objects.requireNonNull(updateOrganizationUseCase);
		this.archiveOrganizationUseCase = Objects.requireNonNull(archiveOrganizationUseCase);
		this.createTeamUseCase = Objects.requireNonNull(createTeamUseCase);
		this.listTeamsForOrganizationUseCase = Objects.requireNonNull(listTeamsForOrganizationUseCase);
		this.createOrganizationInvitationUseCase = Objects.requireNonNull(createOrganizationInvitationUseCase);
		this.listOrganizationInvitationsUseCase = Objects.requireNonNull(listOrganizationInvitationsUseCase);
		this.revokeInvitationUseCase = Objects.requireNonNull(revokeInvitationUseCase);
		this.listOrganizationMembershipsUseCase = Objects.requireNonNull(listOrganizationMembershipsUseCase);
		this.removeOrganizationMemberUseCase = Objects.requireNonNull(removeOrganizationMemberUseCase);
		this.leaveOrganizationUseCase = Objects.requireNonNull(leaveOrganizationUseCase);
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	OrganizationResponse create(@Valid @RequestBody CreateOrganizationRequest request, Authentication authentication) {
		return OrganizationResponse.from(
				createOrganizationUseCase.execute(OrganizationWebSupport.accountId(authentication), request.name()));
	}

	@GetMapping
	List<OrganizationResponse> list(Authentication authentication) {
		return listOrganizationsForAccountUseCase.execute(OrganizationWebSupport.accountId(authentication)).stream()
				.map(OrganizationResponse::from)
				.toList();
	}

	@GetMapping("/{organizationId}")
	OrganizationResponse get(@PathVariable UUID organizationId, Authentication authentication) {
		return OrganizationResponse.from(getOrganizationUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				OrganizationId.of(organizationId)));
	}

	@PatchMapping("/{organizationId}")
	OrganizationResponse update(
			@PathVariable UUID organizationId,
			@Valid @RequestBody UpdateOrganizationRequest request,
			Authentication authentication) {
		return OrganizationResponse.from(updateOrganizationUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				OrganizationId.of(organizationId),
				request.name(),
				request.expectedVersion()));
	}

	@PostMapping("/{organizationId}/archive")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void archive(@PathVariable UUID organizationId, Authentication authentication) {
		archiveOrganizationUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				OrganizationId.of(organizationId));
	}

	@PostMapping("/{organizationId}/teams")
	@ResponseStatus(HttpStatus.CREATED)
	TeamResponse createTeam(
			@PathVariable UUID organizationId,
			@Valid @RequestBody CreateTeamRequest request,
			Authentication authentication) {
		return TeamResponse.from(createTeamUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				OrganizationId.of(organizationId),
				request.name()));
	}

	@GetMapping("/{organizationId}/teams")
	List<TeamResponse> listTeams(@PathVariable UUID organizationId, Authentication authentication) {
		return listTeamsForOrganizationUseCase
				.execute(OrganizationWebSupport.accountId(authentication), OrganizationId.of(organizationId))
				.stream()
				.map(TeamResponse::from)
				.toList();
	}

	@PostMapping("/{organizationId}/invitations")
	@ResponseStatus(HttpStatus.CREATED)
	InvitationResponse createInvitation(
			@PathVariable UUID organizationId,
			@Valid @RequestBody CreateInvitationRequest request,
			Authentication authentication) {
		return InvitationResponse.from(createOrganizationInvitationUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				OrganizationId.of(organizationId),
				request.email(),
				request.role()));
	}

	@GetMapping("/{organizationId}/invitations")
	List<InvitationResponse> listInvitations(@PathVariable UUID organizationId, Authentication authentication) {
		return listOrganizationInvitationsUseCase
				.execute(OrganizationWebSupport.accountId(authentication), OrganizationId.of(organizationId))
				.stream()
				.map(InvitationResponse::withoutToken)
				.toList();
	}

	@PostMapping("/{organizationId}/invitations/{invitationId}/revoke")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void revokeInvitation(
			@PathVariable UUID organizationId,
			@PathVariable UUID invitationId,
			Authentication authentication) {
		revokeInvitationUseCase.revokeOrganizationInvitation(
				OrganizationWebSupport.accountId(authentication),
				OrganizationId.of(organizationId),
				InvitationId.of(invitationId));
	}

	@GetMapping("/{organizationId}/memberships")
	List<OrganizationMembershipResponse> listMemberships(
			@PathVariable UUID organizationId,
			Authentication authentication) {
		return listOrganizationMembershipsUseCase
				.execute(OrganizationWebSupport.accountId(authentication), OrganizationId.of(organizationId))
				.stream()
				.map(OrganizationMembershipResponse::from)
				.toList();
	}

	@DeleteMapping("/{organizationId}/memberships/{membershipId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void removeMembership(
			@PathVariable UUID organizationId,
			@PathVariable UUID membershipId,
			Authentication authentication) {
		removeOrganizationMemberUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				OrganizationId.of(organizationId),
				OrganizationMembershipId.of(membershipId));
	}

	@PostMapping("/{organizationId}/memberships/me/leave")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void leave(@PathVariable UUID organizationId, Authentication authentication) {
		leaveOrganizationUseCase.execute(
				OrganizationWebSupport.accountId(authentication),
				OrganizationId.of(organizationId));
	}

}
